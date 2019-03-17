package de.cronn.reflection.util.immutable.collection;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import de.cronn.reflection.util.immutable.Immutable;
import de.cronn.reflection.util.immutable.ImmutableProxy;

public class DeepImmutableMap<K, V> extends AbstractMap<K, V> implements Immutable, Serializable {

	private static final long serialVersionUID = 1L;

	static final String IMMUTABLE_MESSAGE = "This map is immutable";

	private final Map<K, V> delegate;

	private final Map<K, K> immutableKeyCache = new IdentityHashMap<>();
	private final Map<V, V> immutableValueCache = new IdentityHashMap<>();

	public DeepImmutableMap(Map<K, V> delegate) {
		this.delegate = delegate;
	}

	K getImmutableKey(K key) {
		return immutableKeyCache.computeIfAbsent(key, ImmutableProxy::create);
	}

	V getImmutableValue(V value) {
		return immutableValueCache.computeIfAbsent(value, ImmutableProxy::create);
	}

	@Override
	public V get(Object key) {
		V value = delegate.get(key);
		return getImmutableValue(value);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new DeepImmutableEntrySet<>(delegate.entrySet(), this);
	}

	@Override
	public V put(K key, V value) {
		throw new UnsupportedOperationException(IMMUTABLE_MESSAGE);
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException(IMMUTABLE_MESSAGE);
	}

}
