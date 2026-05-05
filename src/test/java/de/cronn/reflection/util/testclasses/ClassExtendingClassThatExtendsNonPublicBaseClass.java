package de.cronn.reflection.util.testclasses;

import de.cronn.reflection.util.PropertyUtils;
import java.beans.PropertyDescriptor;

public class ClassExtendingClassThatExtendsNonPublicBaseClass
    extends ClassExtendingNonPublicBaseClass {

  public static PropertyDescriptor getPropertyDescriptor() {
    return PropertyUtils.getPropertyDescriptor(
        ClassExtendingClassThatExtendsNonPublicBaseClass.class,
        NonPublicBaseClass::getBaseClassProperty);
  }
}
