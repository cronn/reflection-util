package de.cronn.reflection.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.VisibleForTesting;
import org.objenesis.ObjenesisHelper;

import net.bytebuddy.ByteBuddy;

final class RecordSupport {

	private static volatile boolean currentJvmIsKnownNotToSupportRecords = false;
	private static WeakReference<Class<?>> cachedRecordClass = new WeakReference<>(null);

	private static final ClassValue<Class<?>> dummySubclasses = ClassValues.create(RecordSupport::createDummyProxyClass);

	private RecordSupport() {
	}

	private static Class<?> createDummyProxyClass(Class<?> type) {
		return new ByteBuddy()
			.subclass(type)
			.make()
			.load(RecordSupport.class.getClassLoader())
			.getLoaded();
	}

	static boolean isRecord(Object bean) {
		return isRecord(bean.getClass());
	}

	static boolean isRecord(Class<?> beanClass) {
		if (currentJvmIsKnownNotToSupportRecords) {
			return false;
		}
		try {
			Class<?> recordClass = getRecordClass();
			return recordClass.isAssignableFrom(beanClass);
		} catch (ClassNotFoundException e) {
			currentJvmIsKnownNotToSupportRecords = true;
			return false;
		}
	}

	private static Class<?> getRecordClass() throws ClassNotFoundException {
		Class<?> recordClass = cachedRecordClass.get();
		if (recordClass == null) {
			recordClass = Class.forName("java.lang.Record");
			cachedRecordClass = new WeakReference<>(recordClass);
		}
		return recordClass;
	}

	static class RecordComponentInfo {
		private final String name;
		private final Class<?> type;
		private final Method accessor;

		RecordComponentInfo(String name, Class<?> type, Method accessor) {
			this.name = name;
			this.type = type;
			this.accessor = accessor;
		}

		public String getName() {
			return name;
		}

		public Class<?> getType() {
			return type;
		}

		public Method getAccessor() {
			return accessor;
		}

		public Object retrieveValueFrom(Object record) {
			try {
				return accessor.invoke(record);
			} catch (ReflectiveOperationException e) {
				throw new ReflectionRuntimeException(e);
			}
		}
	}

	static <T> Method findMethod(Class<T> recordClass, TypedPropertyGetter<T, ?> componentAccessor) {
		Object[] uniqueValues = buildUniqueValues(recordClass);
		try {
			Constructor<T> recordConstructor = getRecordConstructor(recordClass);
			T record = ClassUtils.createInstance(recordConstructor, uniqueValues);

			Object value = componentAccessor.get(record);

			if (needsFallbackToComponentSearch(uniqueValues, value)) {
				return exhaustiveComponentSearch(value, recordClass, componentAccessor, uniqueValues, recordConstructor);
			}

			int componentIndex = ArrayUtils.indexOf(uniqueValues, value);
			Assert.isTrue(componentIndex >= 0,
				() -> "Failed to find a component in " + recordClass.getName() + " for the given component accessor.");
			return getRecordComponentAccessor(recordClass, componentIndex);
		} catch (ReflectiveOperationException e) {
			throw new ReflectionRuntimeException(e);
		}
	}

	static Stream<RecordComponentInfo> getRecordComponents(Class<?> recordClass) {
		Assert.isTrue(isRecord(recordClass), () -> recordClass + " is not a record");
		Object[] recordComponents = invokeMethod(recordClass, "getRecordComponents");
		return Arrays.stream(recordComponents)
			.map(recordComponent -> {
				String name = invokeMethod(recordComponent, "getName");
				Class<?> type = invokeMethod(recordComponent, "getType");
				Method accessor = invokeMethod(recordComponent, "getAccessor");
				return new RecordComponentInfo(name, type, accessor);
			});
	}

	private static <T> T invokeMethod(Object object, String methodName) {
		try {
			Method method = object.getClass().getMethod(methodName);
			return (T) method.invoke(object);
		} catch (ReflectiveOperationException e) {
			throw new ReflectionRuntimeException(e);
		}
	}

	static <T> Constructor<T> getRecordConstructor(Class<T> recordClass) throws NoSuchMethodException {
		Class<?>[] constructorTypes = getRecordComponents(recordClass)
			.map(RecordComponentInfo::getType)
			.toArray(Class[]::new);
		return recordClass.getDeclaredConstructor(constructorTypes);
	}

	private static Object[] buildUniqueValues(Class<?> recordClass) {
		return getRecordComponents(recordClass)
			.map(RecordComponentInfo::getType)
			.map(uniqueValueBuilder())
			.toArray(Object[]::new);
	}

	private static Function<Class<?>, Object> uniqueValueBuilder() {
		Map<Class<?>, Long> index = new IdentityHashMap<>();
		return type -> {
			if (type.isAssignableFrom(boolean.class)) {
				// Note: When the record has more than one primitive boolean component,
				//       we need to fall back to an exhaustive component search via exhaustiveComponentSearch(…)
				return true;
			} else if (type.isPrimitive() || type.isAssignableFrom(String.class) || Number.class.isAssignableFrom(type)) {
				long currentIndex = index.compute(type, (k, value) -> value == null ? 1L : value + 1L);
				if (type.isAssignableFrom(String.class)) {
					return String.valueOf(currentIndex);
				} else if (type.equals(byte.class) || type.equals(Byte.class)) {
					return safeNumberCast(currentIndex, (byte) currentIndex);
				} else if (type.equals(short.class) || type.equals(Short.class)) {
					return safeNumberCast(currentIndex, (short) currentIndex);
				} else if (type.equals(int.class) || type.equals(Integer.class)) {
					return safeNumberCast(currentIndex, (int) currentIndex);
				} else if (type.equals(long.class) || type.equals(Long.class)) {
					return currentIndex;
				} else if (type.equals(float.class) || type.equals(Float.class)) {
					return safeNumberCast(currentIndex, (float) currentIndex);
				} else if (type.equals(double.class) || type.equals(Double.class)) {
					return safeNumberCast(currentIndex, (double) currentIndex);
				} else if (type.equals(char.class)) {
					return safeNumberCast(currentIndex, (char) currentIndex);
				}
			}

			if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
				Class<?> dummyClass = dummySubclasses.get(type);
				return ObjenesisHelper.newInstance(dummyClass);
			}
			return ObjenesisHelper.newInstance(type);
		};
	}

	private static <T extends Number> T safeNumberCast(long currentIndex, T castedValue) {
		return safeNumberCast(currentIndex, castedValue, castedValue.longValue(), castedValue.getClass());
	}

	private static char safeNumberCast(long currentIndex, char castedValue) {
		return safeNumberCast(currentIndex, castedValue, castedValue, char.class);
	}

	private static <T> T safeNumberCast(long currentIndex, T castedValue, long castedValueAsLong, Class<?> valueType) {
		// This is currently not possible to test since a record must not have more than 255 components
		Assert.isTrue(castedValueAsLong == currentIndex,
			() -> "Having more than " + (currentIndex - 1) + " record components of type "
				  + valueType.getName() + " is currently not supported");
		return castedValue;
	}

	private static <T> Method exhaustiveComponentSearch(Object currentValue, Class<T> recordClass,
														TypedPropertyGetter<T, ?> componentAccessor,
														Object[] uniqueValues,
														Constructor<T> recordConstructor) throws ReflectiveOperationException {
		Object[] values = Arrays.copyOf(uniqueValues, uniqueValues.length);

		int nextIndex;
		while ((nextIndex = ArrayUtils.indexOf(values, currentValue)) >= 0) {
			values[nextIndex] = getDefaultValue(currentValue);
			T record = ClassUtils.createInstance(recordConstructor, values);
			Object value = componentAccessor.get(record);
			if (value == values[nextIndex]) {
				return getRecordComponentAccessor(recordClass, nextIndex);
			}
		}
		throw new IllegalArgumentException("Failed to find the component of type " + currentValue.getClass().getName()
										   + " in the record " + recordClass.getName()
										   + " using the provided component accessor.");
	}

	private static Method getRecordComponentAccessor(Class<?> recordClass, int componentIndex) {
		return getRecordComponents(recordClass)
			.skip(componentIndex)
			.findFirst()
			.map(RecordComponentInfo::getAccessor)
			.orElseThrow(IllegalStateException::new);
	}

	private static Object getDefaultValue(Object value) {
		Assert.isTrue(value instanceof Boolean, () -> "This is currently only expected to happen for boolean types");
		return false;
	}

	private static boolean needsFallbackToComponentSearch(Object[] uniqueValues, Object value) {
		if (!(value instanceof Boolean)) {
			return false;
		}
		int firstIndex = ArrayUtils.indexOf(uniqueValues, value);
		int lastIndex = ArrayUtils.lastIndexOf(uniqueValues, value);
		return firstIndex != lastIndex;
	}

	static Collection<PropertyDescriptor> collectPropertyDescriptorsOfRecord(Class<?> type) {
		return Stream.concat(
				Stream.of(getPropertyDescriptorsOfObject()),
				getRecordComponents(type)
					.map(recordComponent -> toPropertyDescriptor(type, recordComponent)))
			.collect(Collectors.toList());
	}

	private static PropertyDescriptor getPropertyDescriptorsOfObject() {
		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(Object.class);
			PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
			Assert.isTrue(propertyDescriptors.length == 1,
				() -> "Expected one property descriptor but got " + propertyDescriptors.length);
			return propertyDescriptors[0];
		} catch (IntrospectionException e) {
			throw new ReflectionRuntimeException(e);
		}
	}

	private static PropertyDescriptor toPropertyDescriptor(Class<?> type, RecordComponentInfo recordComponent) {
		try {
			return new PropertyDescriptor(recordComponent.getName(), type, recordComponent.getAccessor().getName(), null);
		} catch (IntrospectionException e) {
			throw new ReflectionRuntimeException(e);
		}
	}

	@VisibleForTesting
	static final class ArrayUtils {

		private ArrayUtils() {
		}

		static int indexOf(Object[] values, Object valueToFind) {
			for (int i = 0; i < values.length; i++) {
				if (areTheSame(values[i], valueToFind)) {
					return i;
				}
			}
			return -1;
		}

		static int lastIndexOf(Object[] values, Object valueToFind) {
			for (int i = values.length - 1; i >= 0; i--) {
				if (areTheSame(values[i], valueToFind)) {
					return i;
				}
			}
			return -1;
		}

		private static boolean areTheSame(Object one, Object other) {
			if (one instanceof Float || one instanceof Double) {
				return Objects.equals(one, other);
			}
			return one == other;
		}
	}

}
