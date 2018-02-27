package de.cronn.reflection.util;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.objenesis.ObjenesisHelper;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

public final class PropertyUtils {

	private static final Map<Class<?>, PropertyDescriptorCache<?>> cache = new ConcurrentHashMap<>();

	private PropertyUtils() {
	}

	@Nullable
	public static PropertyDescriptor getPropertyDescriptorByName(Object bean, String propertyName) {
		return getPropertyDescriptorByName(ClassUtils.getRealClass(bean), propertyName);
	}

	@Nullable
	public static PropertyDescriptor getPropertyDescriptorByName(Class<?> beanClass, String propertyName) {
		PropertyDescriptorCache<?> propertyDescriptorCache = getCache(beanClass);
		return propertyDescriptorCache.getDescriptorByName(propertyName);
	}

	@Nonnull
	public static PropertyDescriptor getPropertyDescriptorByNameOrThrow(Object bean, String propertyName) {
		Class<Object> beanClass = ClassUtils.getRealClass(bean);
		return getPropertyDescriptorByNameOrThrow(beanClass, propertyName);
	}

	@Nonnull
	public static PropertyDescriptor getPropertyDescriptorByNameOrThrow(Class<?> beanClass, String propertyName) {
		PropertyDescriptor propertyDescriptor = getPropertyDescriptorByName(beanClass, propertyName);
		if (propertyDescriptor == null) {
			throw new IllegalArgumentException(String.format("Property '%s' not found for '%s'", propertyName, beanClass.getSimpleName()));
		}
		return propertyDescriptor;
	}

	public static Collection<PropertyDescriptor> getPropertyDescriptors(Class<?> type) {
		PropertyDescriptorCache<?> propertyDescriptorCache = getCache(type);
		return propertyDescriptorCache.getDescriptors();
	}

	public static Collection<PropertyDescriptor> getPropertyDescriptors(Object object) {
		return getPropertyDescriptors(ClassUtils.getRealClass(object));
	}

	public static <A extends Annotation> Map<PropertyDescriptor, A> getPropertyDescriptorsWithAnnotation(Object object, Class<A> annotationClass) {
		Class<Object> objectClass = ClassUtils.getRealClass(object);
		return getPropertyDescriptorsWithAnnotation(objectClass, annotationClass);
	}

	public static <A extends Annotation> Map<PropertyDescriptor, A> getPropertyDescriptorsWithAnnotation(Class<?> type,
		Class<A> annotationClass) {
		PropertyDescriptorCache<?> propertyDescriptorCache = getCache(type);
		return propertyDescriptorCache.getDescriptorsForAnnotation(annotationClass);
	}

	@SuppressWarnings("unchecked")
	static <T> PropertyDescriptorCache<T> getCache(Class<T> type) {
		return (PropertyDescriptorCache<T>) cache.computeIfAbsent(type, PropertyDescriptorCache::compute);
	}

	public static <T> T copyNonDefaultValues(T source, T destination) {
		getPropertyDescriptors(source).stream()
			.filter(PropertyUtils::isFullyAccessible)
			.filter(propertyDescriptor -> !hasDefaultValue(source, propertyDescriptor))
			.forEach(propertyDescriptor -> copyValue(source, destination, propertyDescriptor));
		return destination;
	}

	public static <T> Object copyValue(T source, T destination, PropertyDescriptor propertyDescriptor) {
		Object value = read(source, propertyDescriptor);
		write(destination, propertyDescriptor, value);
		return value;
	}

	public static <T> boolean hasDefaultValue(T bean, PropertyDescriptor propertyDescriptor) {
		Object value = read(bean, propertyDescriptor);
		Class<?> beanClass = ClassUtils.getRealClass(bean);
		return isDefaultValue(beanClass, propertyDescriptor, value);
	}

	public static <T> boolean hasSameValue(T a, T b, PropertyDescriptor propertyDescriptor) {
		Object valueFromA = read(a, propertyDescriptor);
		Object valueFromB = read(b, propertyDescriptor);
		return Objects.equals(valueFromA, valueFromB);
	}

	public static <T> boolean hasDifferentValue(T a, T b, PropertyDescriptor propertyDescriptor) {
		return !hasSameValue(a, b, propertyDescriptor);
	}

	public static <T> boolean isDefaultValue(Class<T> objectClass, PropertyGetter<T> propertyGetter, Object value) {
		return isDefaultValue(objectClass, getPropertyDescriptor(objectClass, propertyGetter), value);
	}

	public static <T> boolean isDefaultValue(Class<T> objectClass, PropertyDescriptor propertyDescriptor, Object value) {
		Object defaultValue = getDefaultValue(objectClass, propertyDescriptor);
		return Objects.equals(value, defaultValue);
	}

	public static <T> Object getDefaultValue(Class<T> objectClass, PropertyDescriptor propertyDescriptor) {
		return getCache(objectClass).getDefaultValue(propertyDescriptor);
	}

	public static void write(Object destination, String propertyName, Object value) {
		PropertyDescriptor propertyDescriptor = getPropertyDescriptorByNameOrThrow(destination, propertyName);
		write(destination, propertyDescriptor, value);
	}

	public static <T> void writeIfPropertyExists(Object destination, String propertyName, Supplier<T> valueSupplier) {
		PropertyDescriptor property = getPropertyDescriptorByName(destination, propertyName);
		if (property != null) {
			T value = valueSupplier.get();
			write(destination, property, value);
		}
	}

	public static void write(Object destination, PropertyDescriptor propertyDescriptor, Object value) {
		write(destination, propertyDescriptor, value, false);
	}

	public static void write(Object destination, PropertyDescriptor propertyDescriptor, Object value, boolean force) {
		try {
			if (!isWritable(propertyDescriptor)) {
				if (force) {
					writeDirectly(destination, propertyDescriptor, value);
				} else {
					throw new IllegalArgumentException(propertyDescriptor.getName() + " is not writable");
				}
			} else {
				Object[] args = new Object[] { value };
				Method writeMethod = propertyDescriptor.getWriteMethod();
				writeMethod.invoke(destination, args);
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			throw new ReflectionRuntimeException("Failed to write " + propertyDescriptor.getName() + " to " + destination, e);
		}
	}

	public static void writeDirectly(Object destination, PropertyDescriptor propertyDescriptor, Object value) {
		try {
			Field field = findField(destination, propertyDescriptor);
			boolean accessible = field.isAccessible();
			try {
				if (!accessible) {
					field.setAccessible(true);
				}
				field.set(destination, value);
			} finally {
				if (!accessible) {
					field.setAccessible(false);
				}
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new ReflectionRuntimeException("Failed to write " + getQualifiedPropertyName(destination, propertyDescriptor), e);
		}
	}

	private static Field findField(Object object, PropertyDescriptor propertyDescriptor) throws NoSuchFieldException {
		Class<Object> objectClass = ClassUtils.getRealClass(object);
		return findField(objectClass, propertyDescriptor.getName());
	}

	private static Field findField(Class<Object> objectClass, String propertyName) throws NoSuchFieldException {
		try {
			return objectClass.getDeclaredField(propertyName);
		} catch (NoSuchFieldException e) {
			Class<? super Object> superclass = objectClass.getSuperclass();
			if (!superclass.equals(Object.class)) {
				return findField(superclass, propertyName);
			}
			throw e;
		}
	}

	public static <T> T readDirectly(Object object, PropertyDescriptor propertyDescriptor) {
		try {
			Field field = findField(object, propertyDescriptor);
			boolean accessible = field.isAccessible();
			try {
				if (!accessible) {
					field.setAccessible(true);
				}
				@SuppressWarnings("unchecked")
				T value = (T) field.get(object);
				return value;
			} finally {
				if (!accessible) {
					field.setAccessible(false);
				}
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new ReflectionRuntimeException("Failed to read " + getQualifiedPropertyName(object, propertyDescriptor), e);
		}
	}

	public static <T> T read(Object source, PropertyDescriptor propertyDescriptor) {
		if (!isReadable(propertyDescriptor)) {
			throw new IllegalArgumentException(String.format("%s must be readable", propertyDescriptor.getName()));
		}
		final Object result;
		try {
			Method readMethod = propertyDescriptor.getReadMethod();
			result = readMethod.invoke(source);
		} catch (ReflectiveOperationException | RuntimeException e) {
			throw new ReflectionRuntimeException("Failed to read " + getQualifiedPropertyName(source, propertyDescriptor), e);
		}
		@SuppressWarnings("unchecked")
		T castedResult = (T) result;
		return castedResult;
	}

	public static <T> T readIfPropertyExists(Object source, String propertyName) {
		PropertyDescriptor property = getPropertyDescriptorByName(source, propertyName);
		if (property != null) {
			return read(source, property);
		} else {
			return null;
		}
	}

	public static <T> T readProperty(Object entity, PropertyDescriptor propertyDescriptor, Class<T> expectedType) {
		Class<?> clazz = ClassUtils.getRealClass(entity);
		String propertyName = propertyDescriptor.getName();
		Class<?> propertyType = propertyDescriptor.getPropertyType();
		if (!expectedType.isAssignableFrom(propertyType)) {
			throw new IllegalArgumentException(String.format("%s.%s is of type %s but %s is expected", clazz, propertyName, propertyType, expectedType));
		}
		T value = PropertyUtils.read(entity, propertyDescriptor);
		return value;
	}

	@Nonnull
	public static <T> PropertyDescriptor getPropertyDescriptor(T bean, PropertyGetter<T> propertyGetter) {
		Class<T> beanClass = ClassUtils.getRealClass(bean);
		return getPropertyDescriptor(beanClass, propertyGetter);
	}

	@Nonnull
	public static <T> PropertyDescriptor getPropertyDescriptor(Class<T> beanClass, PropertyGetter<T> propertyGetter) {
		Method method = getMethod(beanClass, propertyGetter);
		PropertyDescriptor propertyDescriptor = getPropertyDescriptorByMethod(beanClass, method);
		if (propertyDescriptor == null) {
			throw new IllegalArgumentException(String.format("Found no property for %s on class %s", method, beanClass));
		}
		return propertyDescriptor;
	}

	@Nonnull
	public static <T> String getPropertyName(Class<T> beanClass, PropertyGetter<T> propertyGetter) {
		PropertyDescriptor propertyDescriptor = getPropertyDescriptor(beanClass, propertyGetter);
		return propertyDescriptor.getName();
	}

	@Nonnull
	public static <T> String getPropertyName(T bean, PropertyGetter<T> propertyGetter) {
		Class<T> beanClass = ClassUtils.getRealClass(bean);
		return getPropertyName(beanClass, propertyGetter);
	}

	@Nullable
	public static <T> PropertyDescriptor getPropertyDescriptorByMethod(Class<T> beanClass, Method method) {
		PropertyDescriptorCache<?> propertyDescriptorCache = getCache(beanClass);
		return propertyDescriptorCache.getDescriptorByMethod(method);
	}

	@Nullable
	public static <T> PropertyDescriptor getPropertyDescriptorByField(Class<T> beanClass, Field field) {
		PropertyDescriptorCache<?> propertyDescriptorCache = getCache(beanClass);
		return propertyDescriptorCache.getDescriptorByField(field);
	}

	@Nonnull
	public static <T> Method getMethod(Class<T> beanClass, PropertyGetter<T> propertyGetter) {
		PropertyDescriptorCache<T> cache = getCache(beanClass);
		return cache.getMethod(propertyGetter);
	}

	private static class MethodCaptor implements InvocationHandler {

		private final AtomicReference<Method> capturedMethod = new AtomicReference<>();

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Method existing = capturedMethod.getAndSet(method);
			if (existing != null) {
				throw new IllegalArgumentException(String.format("Method already captured: %s called twice?", existing));
			}

			return getDefaultValueObject(method.getReturnType());
		}

		Method getCapturedMethod() {
			Method method = capturedMethod.get();
			if (method == null) {
				throw new IllegalArgumentException("Method could not be captured. This can happen when no method was invoked or the method is private or final.");
			}
			return method;
		}

	}

	static <T> Method findMethodByGetter(Class<T> beanClass, PropertyGetter<T> propertyGetter) {
		MethodCaptor methodCaptor = new MethodCaptor();
		T proxy = createProxy(beanClass, methodCaptor);

		propertyGetter.get(proxy);

		return methodCaptor.getCapturedMethod();
	}

	private static <T> T createProxy(Class<T> beanClass, InvocationHandler invocationHandler) {
		Class<? extends T> proxyClass = new ByteBuddy()
			.subclass(beanClass, ConstructorStrategy.Default.NO_CONSTRUCTORS)
			.method(isMethod()
				.and(takesArguments(0))
				.and(not(isDeclaredBy(Object.class))))
			.intercept(InvocationHandlerAdapter.of(invocationHandler))
			.make()
			.load(PropertyUtils.class.getClassLoader())
			.getLoaded();

		return ObjenesisHelper.newInstance(proxyClass);
	}

	public static boolean hasAnnotationOfProperty(Class<?> entityType, PropertyDescriptor descriptor, Class<? extends Annotation> annotationClass) {
		return getAnnotationOfProperty(entityType, descriptor, annotationClass) != null;
	}

	public static <T, A extends Annotation> A getAnnotationOfProperty(Class<T> entityType, PropertyGetter<T> propertyGetter, Class<A> annotationClass) {
		PropertyDescriptor propertyDescriptor = getPropertyDescriptor(entityType, propertyGetter);
		return getAnnotationOfProperty(entityType, propertyDescriptor, annotationClass);
	}

	public static <A extends Annotation> A getAnnotationOfProperty(Object object, PropertyDescriptor descriptor, Class<A> annotationClass) {
		Class<Object> objectClass = ClassUtils.getRealClass(object);
		return getAnnotationOfProperty(objectClass, descriptor, annotationClass);
	}

	public static <A extends Annotation> A getAnnotationOfProperty(Class<?> entityType, PropertyDescriptor descriptor, Class<A> annotationClass) {
		PropertyDescriptorCache<?> cache = getCache(entityType);
		Map<PropertyDescriptor, A> descriptorsForAnnotation = cache.getDescriptorsForAnnotation(annotationClass);
		return descriptorsForAnnotation.get(descriptor);
	}

	public static boolean isFullyAccessible(PropertyDescriptor descriptor) {
		return isReadable(descriptor) && isWritable(descriptor);
	}

	public static boolean isWritable(PropertyDescriptor descriptor) {
		return descriptor.getWriteMethod() != null;
	}

	public static boolean isReadable(PropertyDescriptor descriptor) {
		return descriptor.getReadMethod() != null;
	}

	public static boolean isDeclaredInClass(PropertyDescriptor propertyDescriptor, Class<?> entityClass) {
		Method readMethod = propertyDescriptor.getReadMethod();
		return readMethod != null && Objects.equals(readMethod.getDeclaringClass(), entityClass);
	}

	public static boolean hasProperty(Object bean, String propertyName) {
		return getPropertyDescriptorByName(bean, propertyName) != null;
	}

	public static boolean hasProperty(Class<?> beanClass, String propertyName) {
		return getPropertyDescriptorByName(beanClass, propertyName) != null;
	}

	public static Object getDefaultValueObject(Class<?> type) {
		if (type.isPrimitive()) {
			if (type.equals(long.class)) {
				return Long.valueOf(0);
			} else if (type.equals(int.class)) {
				return Integer.valueOf(0);
			} else if (type.equals(boolean.class)) {
				return Boolean.valueOf(false);
			} else if (type.equals(void.class)) {
				return null;
			} else {
				throw new IllegalArgumentException("Unhandled primitive type: " + type);
			}
		}

		return null;
	}

	public static <T> String getQualifiedPropertyName(T bean, PropertyGetter<T> propertyGetter) {
		Class<T> beanClass = ClassUtils.getRealClass(bean);
		return getQualifiedPropertyName(beanClass, propertyGetter);
	}

	public static <T> String getQualifiedPropertyName(Class<T> type, PropertyGetter<T> propertyGetter) {
		PropertyDescriptor propertyDescriptor = getPropertyDescriptor(type, propertyGetter);
		return getQualifiedPropertyName(type, propertyDescriptor);
	}

	public static String getQualifiedPropertyName(Object bean, PropertyDescriptor propertyDescriptor) {
		return getQualifiedPropertyName(ClassUtils.getRealClass(bean), propertyDescriptor);
	}

	public static String getQualifiedPropertyName(Class<?> type, PropertyDescriptor propertyDescriptor) {
		return type.getSimpleName() + "." + propertyDescriptor.getName();
	}

	public static boolean isCollectionType(PropertyDescriptor propertyDescriptor) {
		return Collection.class.isAssignableFrom(propertyDescriptor.getPropertyType());
	}

	public static boolean isNotCollectionType(PropertyDescriptor propertyDescriptor) {
		return !isCollectionType(propertyDescriptor);
	}
}
