package de.cronn.reflection.util;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Constructor;

final class TestUtils {

	private TestUtils() {
	}

	static void assertThatConstructorIsPrivate(Class<?> utilityClass) throws Exception {
		Constructor<?> constructor = utilityClass.getDeclaredConstructor();
		assertThat(constructor.canAccess(null)).isFalse();
		constructor.setAccessible(true);
		Object instance = constructor.newInstance();
		assertThat(instance).isNotNull();
	}

}
