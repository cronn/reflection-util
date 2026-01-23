package de.cronn.reflection.util.testclasses;

public interface UserDetails {
	default boolean isEnabled() {
		return true;
	}
}
