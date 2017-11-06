package de.cronn.reflection.util;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class MethodSignatureTest {

	@Test
	public void testEquals() throws Exception {
		EqualsVerifier.forClass(MethodSignature.class).verify();
	}

}