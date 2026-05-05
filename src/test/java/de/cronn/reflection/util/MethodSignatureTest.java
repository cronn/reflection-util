package de.cronn.reflection.util;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class MethodSignatureTest {

  @Test
  void testEquals() {
    EqualsVerifier.forClass(MethodSignature.class).verify();
  }
}
