package de.cronn.reflection.util;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class MethodSignatureTest {

	@Test
	void testEquals() throws Exception {
		EqualsVerifier.forClass(MethodSignature.class).verify();
	}

}
