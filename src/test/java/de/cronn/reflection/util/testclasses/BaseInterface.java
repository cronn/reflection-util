package de.cronn.reflection.util.testclasses;

import javax.validation.constraints.Size;

public interface BaseInterface {

	@Size(max = 40)
	String getSizeFromInterface();

}
