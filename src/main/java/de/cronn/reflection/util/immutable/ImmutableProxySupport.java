package de.cronn.reflection.util.immutable;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Path;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.jetbrains.annotations.UnmodifiableView;
import org.objenesis.ObjenesisHelper;

import de.cronn.reflection.util.ClassUtils;
import de.cronn.reflection.util.PropertyUtils;
import de.cronn.reflection.util.immutable.collection.DeepImmutableCollection;
import de.cronn.reflection.util.immutable.collection.DeepImmutableList;
import de.cronn.reflection.util.immutable.collection.DeepImmutableMap;
import de.cronn.reflection.util.immutable.collection.DeepImmutableSet;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.ExceptionMethod;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

abstract class ImmutableProxySupport {

	static final String DELEGATE_FIELD_NAME = "$delegate";
	static final String COLLECTION_DELEGATE_FIELD_NAME = "delegate";

	static <T> T create(T instance, boolean softImmutable, Function<T, T> proxyGenerator) {
		if (isImmutable(instance, softImmutable)) {
			return instance;
		} else if (instance instanceof List) {
			@SuppressWarnings("unchecked")
			T immutableList = (T) create((List<?>) instance, softImmutable);
			return immutableList;
		} else if (instance instanceof Set) {
			@SuppressWarnings("unchecked")
			T immutableSet = (T) create((Set<?>) instance, softImmutable);
			return immutableSet;
		} else if (instance instanceof Map) {
			@SuppressWarnings("unchecked")
			T immutableMap = (T) create((Map<?, ?>) instance, softImmutable);
			return immutableMap;
		}
		return proxyGenerator.apply(instance);
	}

	@UnmodifiableView
	static <T> Collection<T> create(Collection<T> collection, boolean softImmutable) {
		return new DeepImmutableCollection<>(collection, softImmutable);
	}

	@UnmodifiableView
	static <T> List<T> create(List<T> list, boolean softImmutable) {
		return new DeepImmutableList<>(list, softImmutable);
	}

	@UnmodifiableView
	static <T> Set<T> create(Set<T> set, boolean softImmutable) {
		return new DeepImmutableSet<>(set, softImmutable);
	}

	@UnmodifiableView
	static <K, V> Map<K, V> create(Map<K, V> map, boolean softImmutable) {
		return new DeepImmutableMap<>(map, softImmutable);
	}

	static <T> T unwrap(T immutableProxy) {
		if (!isImmutableProxy(immutableProxy)) {
			return immutableProxy;
		} else if (immutableProxy instanceof DeepImmutableMap || immutableProxy instanceof DeepImmutableCollection) {
			return PropertyUtils.readDirectly(immutableProxy, COLLECTION_DELEGATE_FIELD_NAME);
		} else {
			return PropertyUtils.readDirectly(immutableProxy, DELEGATE_FIELD_NAME);
		}
	}

	static boolean isImmutableProxy(Object object) {
		if (object == null) {
			return false;
		}
		return isImmutableProxyClass(object.getClass());
	}

	static boolean isImmutableProxyClass(Class<?> beanClass) {
		return Immutable.class.isAssignableFrom(beanClass);
	}

	static boolean isImmutable(Object value, boolean softImmutable) {
		if (value == null) {
			return true;
		} else if (isImmutableProxy(value)) {
			return true;
		} else if (value instanceof String) {
			return true;
		} else if (value instanceof Number) {
			return true;
		} else if (value instanceof Boolean) {
			return true;
		} else if (value instanceof Character) {
			return true;
		} else if (value instanceof Temporal) {
			return true;
		} else if (value instanceof TemporalAmount) {
			return true;
		} else if (value instanceof UUID) {
			return true;
		} else if (value instanceof File) {
			return true;
		} else if (value instanceof Path) {
			return true;
		} else if (value instanceof URI) {
			return true;
		} else if (isEnumValue(value)) {
			return true;
		} else if (softImmutable && isFinal(value)) {
			return true;
		} else {
			return false;
		}
	}

	private static boolean isEnumValue(Object value) {
		return value.getClass().isEnum()
			   || (value.getClass().getSuperclass() != null && value.getClass().getSuperclass().isEnum());
	}

	private static boolean isFinal(Object value) {
		return Modifier.isFinal(value.getClass().getModifiers());
	}

	static <T> T getProxy(T instance, Map<Class<?>, Class<?>> cache, Class<?> forwarderClass) {
		Class<? extends T> proxyClass = getOrCreateProxyClass(instance, cache, forwarderClass);
		T proxy = ObjenesisHelper.newInstance(proxyClass);
		PropertyUtils.writeDirectly(proxy, DELEGATE_FIELD_NAME, instance);
		return proxy;
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<? extends T> getOrCreateProxyClass(T instance, Map<Class<?>, Class<?>> cache, Class<?> forwarderClass) {
		Class<T> realClass = ClassUtils.getRealClass(instance);
		return (Class<? extends T>) cache.computeIfAbsent(realClass, clazz -> createProxyClass((Class<T>) clazz, forwarderClass));
	}

	private static <T> Class<? extends T> createProxyClass(Class<T> clazz, Class<?> forwarderClass) {
		return new ByteBuddy()
			.subclass(clazz)
			.implement(Immutable.class)
			.defineField(DELEGATE_FIELD_NAME, clazz)
			.method(any())
			.intercept(ExceptionMethod.throwing(UnsupportedOperationException.class, "This instance is immutable."
																					 + " Annotate the method with @" + ReadOnly.class.getSimpleName() + " if this is a false-positive."))
			.method(isReadyOnlyMethod())
			.intercept(MethodDelegation.to(forwarderClass))
			.method(isReadyOnlyMethod().and(returns(Long.class).or(returns(long.class))))
			.intercept(MethodDelegation.to(ImmutableProxyForwarderLong.class))
			.method(isReadyOnlyMethod().and(returns(Integer.class).or(returns(int.class))))
			.intercept(MethodDelegation.to(ImmutableProxyForwarderInteger.class))
			.method(isReadyOnlyMethod().and(returns(Boolean.class).or(returns(boolean.class))))
			.intercept(MethodDelegation.to(ImmutableProxyForwarderBoolean.class))
			.method(isReadyOnlyMethod().and(returns(String.class)))
			.intercept(MethodDelegation.to(ImmutableProxyForwarderString.class))
			.make()
			.load(ImmutableProxy.class.getClassLoader())
			.getLoaded();
	}

	private static ElementMatcher.Junction<MethodDescription> isReadyOnlyMethod() {
		return not(isSetter())
			.and(isGetter()
				.or(isHashCode()).or(isEquals()).or(isToString()).or(isClone())
				.or(isDeclaredBy(Object.class))
				.or(isAnnotatedWith(ReadOnly.class)));
	}

	private static ElementMatcher<MethodDescription> isAnnotatedWith(Class<? extends Annotation> annotation) {
		return target -> {
			TypeDefinition type = target.getDeclaringType();
			MethodDescription.SignatureToken methodSignature = target.asSignatureToken();
			return isAnnotatedWith(methodSignature, type, annotation);
		};
	}

	private static boolean isAnnotatedWith(MethodDescription.SignatureToken methodSignature, TypeDefinition type, Class<? extends Annotation> annotation) {
		if (type == null || type.equals(TypeDescription.OBJECT)) {
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

	private static boolean hasMethodAnnotatedWith(MethodDescription.SignatureToken methodSignature, TypeDefinition type, Class<? extends Annotation> annotation) {
		return !type.getDeclaredMethods()
			.filter(hasMethodName(methodSignature.getName())
				.and(takesArguments(methodSignature.getParameterTypes()))
				.and(ElementMatchers.isAnnotatedWith(annotation)))
			.isEmpty();
	}

}
