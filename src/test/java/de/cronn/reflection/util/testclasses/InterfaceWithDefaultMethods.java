package de.cronn.reflection.util.testclasses;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import de.cronn.reflection.util.immutable.ReadOnly;

public interface InterfaceWithDefaultMethods {

	@NotNull
	String getId();

	@Size(min = 15)
	default String getName() {
		return "some name";
	}

	@ReadOnly
	int size();

}
