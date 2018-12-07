package de.cronn.reflection.util.testclasses;

public interface InterfaceWithDefaultMethods {

    String getId();

    default String getName() {
        return "some name";
    }

}
