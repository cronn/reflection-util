package de.cronn.reflection.util;

import static org.junit.Assert.*;

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
			assertEquals("some message", e.getMessage());
		}

		try {
			Assert.isNull(new Object(), () -> "some message: " + 12);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("some message: 12", e.getMessage());
		}
	}

}