package de.cronn.reflection.util;

@FunctionalInterface
public interface VoidMethod<T> {
	void invoke(T bean) throws Exception;
}
