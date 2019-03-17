package de.cronn.reflection.util.testclasses;

import de.cronn.reflection.util.immutable.ReadOnly;

interface InterfaceWithReadOnlyMethods {

	@ReadOnly
	int countSomeList();

}
