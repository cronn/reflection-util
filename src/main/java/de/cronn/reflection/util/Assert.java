package de.cronn.reflection.util;

import java.util.function.Supplier;

final class Assert {

	private Assert() {
	}

	static void isNull(Object value, Supplier<String> message) {
		if (value != null) {
			throw new IllegalArgumentException(message.get());
		}
	}

	static void notNull(Object value, Supplier<String> message) {
		if (value == null) {
			throw new IllegalArgumentException(message.get());
		}
	}

	static void isTrue(boolean condition, Supplier<String> message) {
		if (!condition) {
			throw new IllegalArgumentException(message.get());
		}
	}

}
