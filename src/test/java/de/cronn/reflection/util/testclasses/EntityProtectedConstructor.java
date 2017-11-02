package de.cronn.reflection.util.testclasses;

public class EntityProtectedConstructor {

	protected EntityProtectedConstructor() {
	}

	public static EntityProtectedConstructor newEntity() {
		return new EntityProtectedConstructor();
	}
}