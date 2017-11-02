package de.cronn.reflection.util.testclasses;

import javax.validation.constraints.Size;

@SuppressWarnings("unused")
public abstract class BaseClass implements BaseInterface {

	@Size(max = 30)
	private String baseClassStringProperty;

	@Size(min = 10, max = 20)
	public String otherStringProperty;

	public String getBaseClassStringProperty() {
		return baseClassStringProperty;
	}

	public void setBaseClassStringProperty(String baseClassStringProperty) {
		this.baseClassStringProperty = baseClassStringProperty;
	}

	@Override
	public String getSizeFromInterface() {
		return null;
	}
}
