package de.cronn.reflection.util.testclasses;

import jakarta.validation.constraints.Size;

public interface BaseInterface {

	@Size(max = 40)
	String getSizeFromInterface();

}
