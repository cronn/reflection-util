package de.cronn.reflection.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PropertyDescriptorCache<T> {

	private static final Logger log = LoggerFactory.getLogger(PropertyDescriptorCache.class);

	private final Class<T> type;
	private final Map<String, PropertyDescriptor> propertyDescriptorsByName = new LinkedHashMap<>();
	private final Map<Field, PropertyDescriptor> propertyDescriptorsByField = new LinkedHashMap<>();
	private final Map<Method, PropertyDescriptor> propertyDescriptorsByMethod = new LinkedHashMap<>();
	private final Map<Class<? extends Annotation>, Map<PropertyDescriptor, Annotation>> propertyDescriptorsByAnnotation = new LinkedHashMap<>();
	private final Map<PropertyGetter<T>, Method> methodByPropertyGetterCache = new ConcurrentHashMap<>();
	private final Map<VoidMethod<T>, Method> methodByVoidMethodCache = new ConcurrentHashMap<>();
	private final Map<PropertyDescriptor, Object> defaultValues = new ConcurrentHashMap<>();

	private PropertyDescriptorCache(Class<T> type) {
		this.type = type;

		for (PropertyDescriptor propertyDescriptor : getAllPropertyDescriptors()) {
			propertyDescriptorsByName.put(propertyDescriptor.getName(), propertyDescriptor);

			Method readMethod = propertyDescriptor.getReadMethod();
			if (readMethod != null) {
				propertyDescriptorsByMethod.put(readMethod, propertyDescriptor);
				putAnnotations(propertyDescriptor, readMethod.getAnnotations());
			}

			Method writeMethod = propertyDescriptor.getWriteMethod();
			if (writeMethod != null) {
				propertyDescriptorsByMethod.put(writeMethod, propertyDescriptor);
				putAnnotations(propertyDescriptor, writeMethod.getAnnotations());
			}
		}

		for (Field field : getFields()) {
			PropertyDescriptor propertyDescriptor = propertyDescriptorsByName.get(field.getName());
			if (propertyDescriptor != null) {
				propertyDescriptorsByField.put(field, propertyDescriptor);
				putAnnotations(propertyDescriptor, field.getAnnotations());
			}
		}
	}

	private Set<Field> getFields() {
		List<Field> allFields = new ArrayList<>();
		collectFields(type, allFields);
		allFields.sort(Comparator.comparing(Field::getName));
		return new LinkedHashSet<>(allFields);
	}

	private static void collectFields(Class<?> type, Collection<Field> collectedFields) {
		collectedFields.addAll(Arrays.asList(type.getFields()));
		collectedFields.addAll(Arrays.asList(type.getDeclaredFields()));
		if (!type.equals(Object.class)) {
			Class<?> superclass = type.getSuperclass();
			if (superclass != null) {
				collectFields(superclass, collectedFields);
			}
		}
	}

	private void putAnnotations(PropertyDescriptor propertyDescriptor, Annotation[] annotations) {
		for (Annotation annotation : annotations) {
			propertyDescriptorsByAnnotation.computeIfAbsent(annotation.annotationType(), k -> new LinkedHashMap<>()) //
				.put(propertyDescriptor, annotation);
		}
	}

	private PropertyDescriptor[] getAllPropertyDescriptors() {
		try {
			PropertyDescriptor[] descriptors = Introspector.getBeanInfo(type).getPropertyDescriptors();
			// defensive copy to prevent modification of beanutils' internals
			descriptors = Arrays.copyOf(descriptors, descriptors.length);
			Arrays.sort(descriptors, Comparator.comparing(PropertyDescriptor::getName));
			return descriptors;
		} catch (IntrospectionException e) {
			throw new ReflectionRuntimeException(e);
		}
	}

	Collection<PropertyDescriptor> getDescriptors() {
		return propertyDescriptorsByName.values();
	}

	PropertyDescriptor getDescriptorByMethod(Method method) {
		return propertyDescriptorsByMethod.get(method);
	}

	PropertyDescriptor getDescriptorByField(Field field) {
		return propertyDescriptorsByField.get(field);
	}

	<A extends Annotation> Map<PropertyDescriptor, A> getDescriptorsForAnnotation(Class<A> annotationClass) {
		@SuppressWarnings("unchecked")
		Map<PropertyDescriptor, A> descriptors = (Map<PropertyDescriptor, A>) propertyDescriptorsByAnnotation.getOrDefault(
			annotationClass, Collections.emptyMap());
		return Collections.unmodifiableMap(descriptors);
	}

	static <T> PropertyDescriptorCache<T> compute(Class<T> type) {
		return new PropertyDescriptorCache<>(type);
	}

	PropertyDescriptor getDescriptorByName(String propertyName) {
		return propertyDescriptorsByName.get(propertyName);
	}

	Object getDefaultValue(PropertyDescriptor propertyDescriptor) {
		return defaultValues.computeIfAbsent(propertyDescriptor, this::determineDefaultValue);
	}

	private Object determineDefaultValue(PropertyDescriptor propertyDescriptor) {
		try {
			Object defaultObject = ClassUtils.createNewInstance(type);
			return PropertyUtils.read(defaultObject, propertyDescriptor);
		} catch (RuntimeException e) {
			throw new ReflectionRuntimeException("Failed to determine default value for " + PropertyUtils.getQualifiedPropertyName(type, propertyDescriptor), e);
		}
	}

	Method getMethod(PropertyGetter<T> propertyGetter) {
		assertHasNoDeclaredFields(propertyGetter);
		return methodByPropertyGetterCache.computeIfAbsent(propertyGetter, getter -> PropertyUtils.findMethodByGetter(type, getter));
	}

	Method getMethod(VoidMethod<T> voidMethod) {
		assertHasNoDeclaredFields(voidMethod);
		return methodByVoidMethodCache.computeIfAbsent(voidMethod, m -> PropertyUtils.findMethodByGetter(type, toPropertyGetter(m)));
	}

	private static void assertHasNoDeclaredFields(Object lambda) {
		if (hasDeclaredFields(lambda)) {
			throw new IllegalArgumentException(lambda + " is call site specific");
		}
	}

	private static boolean hasDeclaredFields(Object lambda) {
		return lambda.getClass().getDeclaredFields().length > 0;
	}

	private static <T> PropertyGetter<T> toPropertyGetter(VoidMethod<T> voidMethod) {
		return bean -> {
			try {
				voidMethod.invoke(bean);
			} catch (Exception e) {
				throw new ReflectionRuntimeException(e);
			}
			return null;
		};
	}

}
