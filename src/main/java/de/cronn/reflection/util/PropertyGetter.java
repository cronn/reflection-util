package de.cronn.reflection.util;

@FunctionalInterface
public interface PropertyGetter<T> {
	Object get(T bean);
}
