package de.cronn.reflection.util.testclasses;

import de.cronn.reflection.util.immutable.ReadOnly;

public interface OtherInterfaceWithReadOnlyMethods {

	@ReadOnly
	default int countNothing() {
		return 0;
	}

	OtherInterfaceWithReadOnlyMethods asMyself();

	@ReadOnly(proxyReturnValue = false)
	OtherInterfaceWithReadOnlyMethods asReference();

	@ReadOnly(proxyReturnValue = true)
	default OtherInterfaceWithReadOnlyMethods asReferenceImmutableProxy() {
		return asReference();
	}

}
