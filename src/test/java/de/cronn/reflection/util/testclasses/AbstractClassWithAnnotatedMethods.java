package de.cronn.reflection.util.testclasses;

import de.cronn.reflection.util.immutable.ReadOnly;

public abstract class AbstractClassWithAnnotatedMethods implements OtherInterfaceWithReadOnlyMethods {

	@ReadOnly
	public abstract int countSomeSet();

	@Override
	@ReadOnly
	public abstract AbstractClassWithAnnotatedMethods asMyself();

}
