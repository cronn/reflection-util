package de.cronn.reflection.util.immutable;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.UnmodifiableView;
import org.jetbrains.annotations.VisibleForTesting;
import org.objenesis.ObjenesisHelper;

import de.cronn.reflection.util.ClassUtils;
import de.cronn.reflection.util.ClassValues;
import de.cronn.reflection.util.PropertyUtils;
import de.cronn.reflection.util.RecordUtils;
import de.cronn.reflection.util.immutable.collection.DeepImmutableCollection;
import de.cronn.reflection.util.immutable.collection.DeepImmutableList;
import de.cronn.reflection.util.immutable.collection.DeepImmutableMap;
import de.cronn.reflection.util.immutable.collection.DeepImmutableSet;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodDescription.SignatureToken;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.ExceptionMethod;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;

public final class ImmutableProxy {

	static final String DELEGATE_FIELD_NAME = "$delegate";
	static final String OPTIONS = "$options";

	private static final ClassValue<Class<?>> immutableProxyClassCache = ClassValues.create(ImmutableProxy::createProxyClass);

	private ImmutableProxy() {
	}

	public static <T> T create(T instance, ImmutableProxyOption... options) {
		if (isImmutable(instance)) {
			return instance;
		} else if (instance instanceof List) {
			@SuppressWarnings("unchecked")
			T immutableList = (T) create((List<?>) instance);
			return immutableList;
		} else if (instance instanceof Set) {
			@SuppressWarnings("unchecked")
			T immutableSet = (T) create((Set<?>) instance);
			return immutableSet;
		} else if (instance instanceof Map) {
			@SuppressWarnings("unchecked")
			T immutableMap = (T) create((Map<?, ?>) instance);
			return immutableMap;
		} else if (instance instanceof Record) {
			if (isOptionEnabled(options, ImmutableProxyOption.ALLOW_CLONING_RECORDS)) {
				return RecordUtils.cloneRecord(instance, ImmutableProxy::create);
			} else {
				throw new IllegalArgumentException(
					instance.getClass() + " is a record that potentially contains mutable components."
					+ " Consider using ImmutableProxy.create(bean, "
					+ ImmutableProxyOption.class.getSimpleName() + "." + ImmutableProxyOption.ALLOW_CLONING_RECORDS
					+ ") to enable cloning of such records.");
			}
		}
		Class<? extends T> proxyClass = getOrCreateProxyClass(instance);
		T proxy = ObjenesisHelper.newInstance(proxyClass);
		PropertyUtils.writeDirectly(proxy, DELEGATE_FIELD_NAME, instance);
		if (options != null && options.length > 0) {
			PropertyUtils.writeDirectly(proxy, OPTIONS, options);
		}
		return proxy;
	}

	@UnmodifiableView
	public static <T> Collection<T> create(Collection<T> collection) {
		return new DeepImmutableCollection<>(collection);
	}

	@UnmodifiableView
	public static <T> List<T> create(List<T> list) {
		return new DeepImmutableList<>(list);
	}

	@UnmodifiableView
	public static <T> Set<T> create(Set<T> set) {
		return new DeepImmutableSet<>(set);
	}

	@UnmodifiableView
	public static <K, V> Map<K, V> create(Map<K, V> map) {
		return new DeepImmutableMap<>(map);
	}

	public static <T> T unwrap(T immutableProxy) {
		if (!isImmutableProxy(immutableProxy)) {
			return immutableProxy;
		} else {
			return PropertyUtils.readDirectly(immutableProxy, DELEGATE_FIELD_NAME);
		}
	}

	static boolean isImmutable(Object value) {
		if (value == null) {
			return true;
		}
		return isImmutable(value.getClass());
	}

	public static boolean isImmutable(Class<?> type) {
		if (isImmutableProxyClass(type)) {
			return true;
		} else if (String.class.isAssignableFrom(type)) {
			return true;
		} else if (Byte.class.isAssignableFrom(type) || byte.class.isAssignableFrom(type)) {
			return true;
		} else if (Short.class.isAssignableFrom(type) || short.class.isAssignableFrom(type)) {
			return true;
		} else if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
			return true;
		} else if (Long.class.isAssignableFrom(type) || long.class.isAssignableFrom(type)) {
			return true;
		} else if (Float.class.isAssignableFrom(type) || float.class.isAssignableFrom(type)) {
			return true;
		} else if (Double.class.isAssignableFrom(type) || double.class.isAssignableFrom(type)) {
			return true;
		} else if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
			return true;
		} else if (Character.class.isAssignableFrom(type) || char.class.isAssignableFrom(type)) {
			return true;
		} else if (BigDecimal.class.isAssignableFrom(type)) {
			return true;
		} else if (Temporal.class.isAssignableFrom(type)) {
			return true;
		} else if (TemporalAmount.class.isAssignableFrom(type)) {
			return true;
		} else if (UUID.class.isAssignableFrom(type)) {
			return true;
		} else if (File.class.isAssignableFrom(type)) {
			return true;
		} else if (Path.class.isAssignableFrom(type)) {
			return true;
		} else if (URI.class.isAssignableFrom(type)) {
			return true;
		} else if (isEnumType(type)) {
			return true;
		} else if (type.isRecord()) {
			return RecordUtils.hasOnlyImmutableRecordComponents(type);
		} else {
			return false;
		}
	}

	private static boolean isEnumType(Class<?> type) {
		return type.isEnum() || (type.getSuperclass() != null && type.getSuperclass().isEnum());
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<? extends T> getOrCreateProxyClass(T instance) {
		Class<T> realClass = ClassUtils.getRealClass(instance);
		return (Class<? extends T>) immutableProxyClassCache.get(realClass);
	}

	private static <T> Class<? extends T> createProxyClass(Class<T> clazz) {
		assertPublicMethodsAreNotFinal(clazz);
		try (DynamicType.Unloaded<T> unloadedType = new ByteBuddy()
			.subclass(clazz)
			.implement(Immutable.class)
			.defineField(DELEGATE_FIELD_NAME, clazz)
			.defineField(OPTIONS, ImmutableProxyOption[].class)
			.method(any())
			.intercept(ExceptionMethod.throwing(UnsupportedOperationException.class, "This instance is immutable."
																					 + " Annotate the method with @" + ReadOnly.class.getSimpleName() + " if this is a false-positive."))
			.method(isReadOnlyMethod())
			.intercept(MethodDelegation.to(GenericImmutableProxyForwarder.class))
			.method(isReadOnlyMethod().and(returns(Long.class).or(returns(long.class))))
			.intercept(MethodDelegation.to(ImmutableProxyForwarderLong.class))
			.method(isReadOnlyMethod().and(returns(Integer.class).or(returns(int.class))))
			.intercept(MethodDelegation.to(ImmutableProxyForwarderInteger.class))
			.method(isReadOnlyMethod().and(returns(Boolean.class).or(returns(boolean.class))))
			.intercept(MethodDelegation.to(ImmutableProxyForwarderBoolean.class))
			.method(isReadOnlyMethod().and(returns(String.class)))
			.intercept(MethodDelegation.to(ImmutableProxyForwarderString.class))
			.make()) {
			return unloadedType
				.load(ImmutableProxy.class.getClassLoader())
				.getLoaded();
		}
	}

	private static <T> void assertPublicMethodsAreNotFinal(Class<T> clazz) {
		for (Method method : clazz.getMethods()) {
			if (Modifier.isFinal(method.getModifiers())) {
				if (method.getDeclaringClass().equals(Object.class)
					|| isHashCodeMethod(method)
					|| isEqualsMethod(method)
					|| isToStringMethod(method)
					|| isCloneMethod(method)
					|| method.getDeclaredAnnotation(ReadOnly.class) != null) {
					continue;
				}
				throw new IllegalArgumentException("Cannot create an immutable proxy for " + clazz + ". "
												   + "Method " + method + " is final.");
			}
		}
	}

	private static boolean isHashCodeMethod(Method method) {
		return method.getName().equals("hashCode")
			   && method.getReturnType().equals(int.class)
			   && method.getParameterCount() == 0;
	}

	private static boolean isEqualsMethod(Method method) {
		return method.getName().equals("equals")
			   && method.getReturnType().equals(boolean.class)
			   && method.getParameterCount() == 1
			   && method.getParameterTypes()[0].equals(Object.class);
	}

	static boolean isToStringMethod(Method method) {
		return method.getName().equals("toString")
			   && method.getReturnType().equals(String.class)
			   && method.getParameterCount() == 0;
	}

	static boolean isCloneMethod(Method method) {
		return method.getName().equals("clone")
			   && method.getParameterCount() == 0;
	}

	private static Junction<MethodDescription> isReadOnlyMethod() {
		return not(isSetter())
			.and(isGetter()
				.or(isHashCode()).or(isEquals()).or(isToString()).or(isClone())
				.or(isDeclaredBy(Object.class))
				.or(isAnnotatedWith(ReadOnly.class)));
	}

	private static ElementMatcher<MethodDescription> isAnnotatedWith(Class<? extends Annotation> annotation) {
		return target -> {
			TypeDefinition type = target.getDeclaringType();
			SignatureToken methodSignature = target.asSignatureToken();
			return isAnnotatedWith(methodSignature, type, annotation);
		};
	}

	private static boolean isAnnotatedWith(SignatureToken methodSignature, TypeDefinition type, Class<? extends Annotation> annotation) {
		if (type == null || type.equals(TypeDescription.ForLoadedType.of(Object.class))) {
			return false;
		}

		if (hasMethodAnnotatedWith(methodSignature, type, annotation)) {
			return true;
		}

		Iterable<? extends TypeDefinition> interfaces = type.getInterfaces();
		for (TypeDefinition interfaceType : interfaces) {
			if (hasMethodAnnotatedWith(methodSignature, interfaceType, annotation)) {
				return true;
			}
			for (TypeDescription.Generic interfaceSuperclass : interfaceType.getInterfaces()) {
				if (isAnnotatedWith(methodSignature, interfaceSuperclass, annotation)) {
					return true;
				}
			}
		}

		return isAnnotatedWith(methodSignature, type.getSuperClass(), annotation);
	}

	private static boolean hasMethodAnnotatedWith(SignatureToken methodSignature, TypeDefinition type, Class<? extends Annotation> annotation) {
		return !type.getDeclaredMethods()
			.filter(hasMethodName(methodSignature.getName())
				.and(takesArguments(methodSignature.getParameterTypes()))
				.and(ElementMatchers.isAnnotatedWith(annotation)))
			.isEmpty();
	}

	public static boolean isImmutableProxy(Object object) {
		if (object == null) {
			return false;
		}
		return isImmutableProxyClass(object.getClass());
	}

	public static boolean isImmutableProxyClass(Class<?> beanClass) {
		return Immutable.class.isAssignableFrom(beanClass);
	}

	@VisibleForTesting
	static void removeClassFromCache(Class<?> type) {
		immutableProxyClassCache.remove(type);
	}

	private static boolean isOptionEnabled(ImmutableProxyOption[] options, ImmutableProxyOption optionToTest) {
		if (options == null) {
			return false;
		}
		for (ImmutableProxyOption option : options) {
			if (option == optionToTest) {
				return true;
			}
		}
		return false;
	}
}
