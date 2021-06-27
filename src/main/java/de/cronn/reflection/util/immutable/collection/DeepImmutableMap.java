package de.cronn.reflection.util.immutable.collection;

import static de.cronn.reflection.util.immutable.SoftImmutableProxy.*;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import de.cronn.reflection.util.immutable.Immutable;
import de.cronn.reflection.util.immutable.ImmutableProxy;
import de.cronn.reflection.util.immutable.SoftImmutableProxy;

public class DeepImmutableMap<K, V> extends AbstractMap<K, V> implements Immutable, Serializable {

	private static final long serialVersionUID = 1L;

	static final String IMMUTABLE_MESSAGE = "This map is immutable";

	private final Map<K, V> delegate;
	private final boolean softImmutable;

	private final Map<K, K> immutableKeyCache = new IdentityHashMap<>();
	private final Map<V, V> immutableValueCache = new IdentityHashMap<>();

	public DeepImmutableMap(Map<K, V> delegate) {
		this(delegate, SOFT_IMMUTABLE_DEFAULT);
	}

	public DeepImmutableMap(Map<K, V> delegate, boolean softImmutable) {
		this.delegate = delegate;
		this.softImmutable = softImmutable;
	}

	K getImmutableKey(K key) {
		return immutableKeyCache.computeIfAbsent(key, this::createImmutableElement);
	}

	V getImmutableValue(V value) {
		return immutableValueCache.computeIfAbsent(value, this::createImmutableElement);
	}

	<T> T createImmutableElement(T value) {
		if (softImmutable)
			return SoftImmutableProxy.create(value);
		else
			return ImmutableProxy.create(value);
	}

	@Override
	public V get(Object key) {
		V value = delegate.get(key);
		return getImmutableValue(value);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new DeepImmutableEntrySet<>(delegate.entrySet(), this, softImmutable);
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
