package de.cronn.reflection.util.testclasses;

public class ClassWithDefaultMethods implements InterfaceWithDefaultMethods {

	@Override
	public String getId() {
		return "the id";
	}

	@Override
	public int size() {
		return 0;
	}
}
