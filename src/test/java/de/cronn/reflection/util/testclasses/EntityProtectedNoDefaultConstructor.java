package de.cronn.reflection.util.testclasses;

public class EntityProtectedNoDefaultConstructor {

	private final String someProperty;

	private EntityProtectedNoDefaultConstructor(String someProperty) {
		this.someProperty = someProperty;
	}

	public static EntityProtectedNoDefaultConstructor newEntity() {
		return new EntityProtectedNoDefaultConstructor("");
	}

	public String getSomeProperty() {
		return someProperty;
	}

}