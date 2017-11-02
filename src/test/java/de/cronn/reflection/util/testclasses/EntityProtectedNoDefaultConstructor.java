package de.cronn.reflection.util.testclasses;

public class EntityProtectedNoDefaultConstructor {

	private EntityProtectedNoDefaultConstructor(String unused) {
	}

	public static EntityProtectedNoDefaultConstructor newEntity() {
		return new EntityProtectedNoDefaultConstructor("");
	}
}