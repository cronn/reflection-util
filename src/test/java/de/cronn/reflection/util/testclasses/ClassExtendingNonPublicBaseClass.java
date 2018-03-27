package de.cronn.reflection.util.testclasses;

import java.beans.PropertyDescriptor;

import de.cronn.reflection.util.PropertyUtils;

public class ClassExtendingNonPublicBaseClass extends NonPublicBaseClass {

	public static PropertyDescriptor getPropertyDescriptor() {
		return PropertyUtils.getPropertyDescriptor(ClassExtendingNonPublicBaseClass.class, NonPublicBaseClass::getBaseClassProperty);
	}

}
