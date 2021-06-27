package de.cronn.reflection.util.immutable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.UnmodifiableView;

public abstract class SoftImmutableProxy {

	public static final boolean SOFT_IMMUTABLE_ENABLED = true;
	public static final boolean SOFT_IMMUTABLE_DISABLED = false;
	public static final boolean SOFT_IMMUTABLE_DEFAULT = SOFT_IMMUTABLE_DISABLED;

	private static final Map<Class<?>, Class<?>> immutableProxyClassCache = new ConcurrentHashMap<>();

	public static <T> T create(T instance) {
		return ImmutableProxySupport.create(instance, SOFT_IMMUTABLE_ENABLED, SoftImmutableProxy::getProxy);
	}

	@UnmodifiableView
	public static <T> Collection<T> create(Collection<T> collection) {
		return ImmutableProxySupport.create(collection, SOFT_IMMUTABLE_ENABLED);
	}

	@UnmodifiableView
	public static <T> List<T> create(List<T> list) {
		return ImmutableProxySupport.create(list, SOFT_IMMUTABLE_ENABLED);
	}

	@UnmodifiableView
	public static <T> Set<T> create(Set<T> set) {
		return ImmutableProxySupport.create(set, SOFT_IMMUTABLE_ENABLED);
	}

	@UnmodifiableView
	public static <K, V> Map<K, V> create(Map<K, V> map) {
		return ImmutableProxySupport.create(map, SOFT_IMMUTABLE_ENABLED);
	}

	public static <T> T unwrap(T immutableProxy) {
		return ImmutableProxySupport.unwrap(immutableProxy);
	}

	public static boolean isImmutableProxy(Object object) {
		return ImmutableProxySupport.isImmutableProxy(object);
	}

	public static boolean isImmutableProxyClass(Class<?> beanClass) {
		return ImmutableProxySupport.isImmutableProxyClass(beanClass);
	}

	static void clearCache() {
		immutableProxyClassCache.clear();
	}

	private static <T> T getProxy(T instance) {
		return ImmutableProxySupport.getProxy(instance, immutableProxyClassCache, GenericSoftImmutableProxyForwarder.class);
	}

}
