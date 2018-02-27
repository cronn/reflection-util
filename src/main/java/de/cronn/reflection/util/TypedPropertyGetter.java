package de.cronn.reflection.util;

@FunctionalInterface
public interface TypedPropertyGetter<T, V> {
	V get(T bean);
}
