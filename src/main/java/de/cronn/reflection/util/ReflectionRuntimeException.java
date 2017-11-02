package de.cronn.reflection.util;

public class ReflectionRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	ReflectionRuntimeException(Throwable cause) {
		super(cause);
	}

	ReflectionRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

}
