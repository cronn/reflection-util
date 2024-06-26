package de.cronn.reflection.util;

import java.io.Serial;

public class ReflectionRuntimeException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	ReflectionRuntimeException(Throwable cause) {
		super(cause);
	}

	ReflectionRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

}
