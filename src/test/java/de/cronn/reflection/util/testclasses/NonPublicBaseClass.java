package de.cronn.reflection.util.testclasses;

abstract class NonPublicBaseClass {

	private String baseClassProperty;

	protected NonPublicBaseClass() {
	}

	protected NonPublicBaseClass(String baseClassProperty) {
		this.baseClassProperty = baseClassProperty;
	}

	public void setBaseClassProperty(String baseClassProperty) {
		this.baseClassProperty = baseClassProperty;
	}

	public String getBaseClassProperty() {
		return baseClassProperty;
	}
}
