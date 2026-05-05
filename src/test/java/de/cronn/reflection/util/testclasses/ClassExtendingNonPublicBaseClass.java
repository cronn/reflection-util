package de.cronn.reflection.util.testclasses;

import de.cronn.reflection.util.PropertyUtils;
import java.beans.PropertyDescriptor;

public class ClassExtendingNonPublicBaseClass extends NonPublicBaseClass {

  public ClassExtendingNonPublicBaseClass() {}

  public ClassExtendingNonPublicBaseClass(String baseClassProperty) {
    super(baseClassProperty);
  }

  public static PropertyDescriptor getPropertyDescriptor() {
    return PropertyUtils.getPropertyDescriptor(
        ClassExtendingNonPublicBaseClass.class, NonPublicBaseClass::getBaseClassProperty);
  }
}
