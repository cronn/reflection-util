package de.cronn.reflection.util.testclasses;

import jakarta.validation.constraints.Size;

public class EntityWithOverwrittenAnnotations extends BaseClass {

	@Override
	@Size(max = 20)
	public String getBaseClassStringProperty() {
		return super.getBaseClassStringProperty();
	}

}
