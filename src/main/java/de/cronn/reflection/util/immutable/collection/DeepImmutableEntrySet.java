package de.cronn.reflection.util.immutable.collection;

import java.util.Map.Entry;
import java.util.Set;

class DeepImmutableEntrySet<K, V> extends DeepImmutableCollection<Entry<K, V>> implements Set<Entry<K, V>> {

	private static final long serialVersionUID = 1L;

	private final DeepImmutableMap<K, V> immutableMap;

	DeepImmutableEntrySet(Set<Entry<K, V>> entrySet, DeepImmutableMap<K, V> immutableMap, boolean softImmutable) {
		super(entrySet, DeepImmutableMap.IMMUTABLE_MESSAGE, softImmutable);
		this.immutableMap = immutableMap;
	}

	@Override
	Entry<K, V> createImmutableElement(Entry<K, V> entry) {
		return new ImmutableEntry<>(entry, immutableMap);
	}

	private static class ImmutableEntry<K, V> implements Entry<K, V> {

		private final Entry<K, V> delegate;

		private transient final DeepImmutableMap<K, V> immutableMap;

		ImmutableEntry(Entry<K, V> delegate, DeepImmutableMap<K, V> immutableMap) {
			this.delegate = delegate;
			this.immutableMap = immutableMap;
		}

		@Override
		public K getKey() {
			K key = delegate.getKey();
			return immutableMap.getImmutableKey(key);
		}

		@Override
		public V getValue() {
			V value = delegate.getValue();
			return immutableMap.getImmutableValue(value);
		}

		@Override
		public V setValue(V value) {
			throw new UnsupportedOperationException(DeepImmutableMap.IMMUTABLE_MESSAGE);
		}

	}

}
