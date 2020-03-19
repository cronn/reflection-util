package de.cronn.reflection.util;

import static de.cronn.reflection.util.TestUtils.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AssertTest {

	@Test
	void testConstructor() throws Exception {
		assertThatConstructorIsPrivate(Assert.class);
	}

	@Test
	void testIsNull() throws Exception {
		Assert.isNull(null, null);
		Assert.isNull(null, () -> "some message");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> Assert.isNull("", () -> "some message"))
			.withMessage("some message");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> Assert.isNull("", () -> "some message: " + 12))
			.withMessage("some message: 12");
	}

	@Test
	void testNotNull() throws Exception {
		Assert.notNull("", null);
		Assert.notNull(new Object(), () -> "some message");
		Assert.notNull(123, () -> "some message");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> Assert.notNull(null, () -> "some message"))
			.withMessage("some message");
	}

}
