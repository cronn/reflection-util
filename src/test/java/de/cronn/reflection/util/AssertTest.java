package de.cronn.reflection.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class AssertTest {

	@Test
	public void testIsNull() throws Exception {
		Assert.isNull(null, null);
		Assert.isNull(null, () -> "some message");

		try {
			Assert.isNull("", () -> "some message");
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertThat(e).hasMessage("some message");
		}

		try {
			Assert.isNull(new Object(), () -> "some message: " + 12);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertThat(e).hasMessage("some message: 12");
		}
	}

	@Test
	public void testNotNull() throws Exception {
		Assert.notNull("", null);
		Assert.notNull(new Object(), () -> "some message");
		Assert.notNull(123, () -> "some message");

		try {
			Assert.notNull(null, () -> "some message");
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertThat(e).hasMessage("some message");
		}
	}

}