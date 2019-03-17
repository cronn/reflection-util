package de.cronn.reflection.util.testclasses;

public class ClassWithInheritedDefaultMethods implements SubClassOfInterfaceWithDefaultMethods {

	@Override
	public String getId() {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}
}
