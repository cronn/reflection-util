package de.cronn.reflection.util;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class MethodCaptorTest {

	@Test
	void testCapture_HappyCase() throws Exception {
		MethodCaptor methodCaptor = new MethodCaptor();
		Method method = MethodCaptorTest.class.getDeclaredMethod("testCapture_HappyCase");

		methodCaptor.capture(method);

		assertThat(methodCaptor.getCapturedMethod()).isSameAs(method);
	}

	@Test
	void testCapture_NothingCaptured() throws Exception {
		MethodCaptor methodCaptor = new MethodCaptor();

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(methodCaptor::getCapturedMethod)
			.withMessage("Method could not be captured. This can happen when no method was invoked or the method is private or final.");
	}

	@Test
	void testCapture_Twice() throws Exception {
		MethodCaptor methodCaptor = new MethodCaptor();
		Method method = MethodCaptorTest.class.getDeclaredMethod("testCapture_HappyCase");

		methodCaptor.capture(method);

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> methodCaptor.capture(method))
			.withMessage("Method already captured: " + method + " called twice?");
	}

}
