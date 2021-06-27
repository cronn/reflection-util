package de.cronn.reflection.util.immutable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.cronn.reflection.util.ClassUtils;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatchers;

abstract class ImmutableProxyForwarderSupport {

	private static final Map<Method, Boolean> shouldProxyReturnValueCache = new ConcurrentHashMap<>();

	static Object forward(Method method,
						  Object delegate,
						  Object[] args,
						  boolean softImmutable) throws InvocationTargetException, IllegalAccessException {
		Object value = method.invoke(delegate, args);
		if (ImmutableProxySupport.isImmutable(value, softImmutable)) {
			return value;
		}
		if (!shouldProxyReturnValue(method)) {
			return value;
		}
		if (value instanceof Collection) {
			return createImmutableCollection(value, method, softImmutable);
		} else if (value instanceof Map) {
			return createImmutableMap(value, method, softImmutable);
		} else {
			return ImmutableProxy.create(value);
		}
	}

	private static boolean shouldProxyReturnValue(Method method) {
		return shouldProxyReturnValueCache.computeIfAbsent(method, m -> {
			if (isCloneMethod(m)) {
				return false;
			}
			ReadOnly readOnlyAnnotation = ClassUtils.findAnnotation(m, ReadOnly.class);
			return readOnlyAnnotation == null
				   || readOnlyAnnotation.proxyReturnValue();
		});
	}

	private static boolean isCloneMethod(Method method) {
		return ElementMatchers.isClone().matches(new MethodDescription.ForLoadedMethod(method));
	}

	private static Object createImmutableCollection(Object value, Method method, boolean softImmutable) {
		Class<?> returnType = method.getReturnType();
		if (returnType.equals(Set.class)) {
			Set<?> collection = (Set<?>) value;
			return ImmutableProxySupport.create(collection, softImmutable);
		} else if (returnType.equals(List.class)) {
			List<?> collection = (List<?>) value;
			return ImmutableProxySupport.create(collection, softImmutable);
		} else if (returnType.equals(Collection.class) || returnType.equals(Iterable.class)) {
			Collection<?> collection = (Collection<?>) value;
			return ImmutableProxySupport.create(collection, softImmutable);
		} else {
			throw new UnsupportedOperationException("Cannot create immutable collection for " + describeMethod(method) + "."
													+ " The return type is unknown or too specific: " + returnType + "."
													+ " Consider to define a more generic type: Set/List/Collection");
		}
	}

	private static Object createImmutableMap(Object value, Method method, boolean softImmutable) {
		Class<?> returnType = method.getReturnType();
		if (returnType.equals(Map.class)) {
			Map<?, ?> map = (Map<?, ?>) value;
			return ImmutableProxySupport.create(map, softImmutable);
		} else {
			throw new UnsupportedOperationException("Cannot create immutable map for " + describeMethod(method) + "."
													+ " The return type is unknown or too specific: " + returnType + "."
													+ " Consider to define a more generic type: Map");
		}
	}

	private static String describeMethod(Method method) {
		return method.getDeclaringClass().getSimpleName() + "." + method.getName();
	}

}
