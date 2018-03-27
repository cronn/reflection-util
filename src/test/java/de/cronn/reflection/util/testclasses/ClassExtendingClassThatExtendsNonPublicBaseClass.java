package de.cronn.reflection.util.testclasses;

import java.beans.PropertyDescriptor;

import de.cronn.reflection.util.PropertyUtils;

public class ClassExtendingClassThatExtendsNonPublicBaseClass extends ClassExtendingNonPublicBaseClass {

	public static PropertyDescriptor getPropertyDescriptor() {
		return PropertyUtils.getPropertyDescriptor(ClassExtendingClassThatExtendsNonPublicBaseClass.class, NonPublicBaseClass::getBaseClassProperty);
	}

}
