package de.cronn.reflection.util.testclasses;

import javax.validation.constraints.Size;

public interface InterfaceWithDefaultMethods {

    String getId();

    @Size(min = 15)
    default String getName() {
        return "some name";
    }

}
