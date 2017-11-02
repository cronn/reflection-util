package de.cronn.reflection.util.testclasses;

@SuppressWarnings("unused")
public class DerivedClass extends BaseClass {

	private Long longPropertyWithPackageAccessSetter;

	private String stringProperty;

	public Long getLongPropertyWithPackageAccessSetter() {
		return longPropertyWithPackageAccessSetter;
	}

	void setLongPropertyWithPackageAccessSetter(Long longPropertyWithPackageAccessSetter) {
		this.longPropertyWithPackageAccessSetter = longPropertyWithPackageAccessSetter;
	}

	public String getStringProperty() {
		return stringProperty;
	}

	public void setStringProperty(String stringProperty) {
		this.stringProperty = stringProperty;
	}

	public String getOtherStringProperty() {
		return otherStringProperty;
	}

}
