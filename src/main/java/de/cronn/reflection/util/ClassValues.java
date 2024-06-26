package de.cronn.reflection.util;

import java.util.function.Function;

public final class ClassValues {

	private ClassValues() {
	}

	public static <T> ClassValue<T> create(Function<Class<?>, T> mapper) {
		return new ClassValue<>() {
			@Override
			protected T computeValue(Class<?> type) {
				return mapper.apply(type);
			}
		};
	}

}
