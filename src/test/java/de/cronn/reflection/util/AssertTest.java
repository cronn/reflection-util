package de.cronn.reflection.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class AssertTest {

	@Test
	public void testIsNull() throws Exception {
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
	public void testNotNull() throws Exception {
		Assert.notNull("", null);
		Assert.notNull(new Object(), () -> "some message");
		Assert.notNull(123, () -> "some message");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> Assert.notNull(null, () -> "some message"))
			.withMessage("some message");
	}

}