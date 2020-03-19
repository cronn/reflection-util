package de.cronn.reflection.util;

import static de.cronn.reflection.util.TestUtils.*;
import static org.assertj.core.api.Assertions.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.Size;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import de.cronn.reflection.util.testclasses.BaseClass;
import de.cronn.reflection.util.testclasses.BaseInterface;
import de.cronn.reflection.util.testclasses.ClassExtendingClassThatExtendsNonPublicBaseClass;
import de.cronn.reflection.util.testclasses.ClassExtendingNonPublicBaseClass;
import de.cronn.reflection.util.testclasses.ClassWithDefaultMethods;
import de.cronn.reflection.util.testclasses.ClassWithInheritedDefaultMethods;
import de.cronn.reflection.util.testclasses.ClassWithMethodCaptorField;
import de.cronn.reflection.util.testclasses.ClassWithPrimitives;
import de.cronn.reflection.util.testclasses.DerivedClass;
import de.cronn.reflection.util.testclasses.EntityProtectedNoDefaultConstructor;
import de.cronn.reflection.util.testclasses.FinalClass;
import de.cronn.reflection.util.testclasses.InterfaceWithDefaultMethods;
import de.cronn.reflection.util.testclasses.OtherTestEntity;
import de.cronn.reflection.util.testclasses.SubClassOfInterfaceWithDefaultMethods;
import de.cronn.reflection.util.testclasses.SubclassOfClassWithDefaultMethods;
import de.cronn.reflection.util.testclasses.TestAnnotation;
import de.cronn.reflection.util.testclasses.TestEntity;
import net.bytebuddy.ByteBuddy;

class PropertyUtilsTest {

	@Test
	void testConstructor() throws Exception {
		assertThatConstructorIsPrivate(PropertyUtils.class);
	}

	@Test
	void testGetPropertyDescriptorsOfTestEntityClass() throws Exception {
		List<String> propertyNames = collectPropertyNames(PropertyUtils.getPropertyDescriptors(TestEntity.class));
		assertThat(propertyNames).containsExactly(
			"class",
			"fieldWithAnnotationOnSetter",
			"fieldWithoutGetter",
			"number",
			"otherTestEntity",
			"propertyWithExceptionInGetter",
			"propertyWithoutField",
			"someArrayList",
			"someCollection",
			"someFile",
			"someInstant",
			"someIterable",
			"someList",
			"someMap",
			"someObject",
			"somePath",
			"someSet",
			"someTreeMap",
			"someUri",
			"someUuid",
			"string"
		);
	}

	@Test
	void testGetPropertyDescriptorsOfFinalClass() throws Exception {
		List<String> propertyNames = collectPropertyNames(PropertyUtils.getPropertyDescriptors(FinalClass.class));
		assertThat(propertyNames).containsExactly(
			"class",
			"someProperty"
		);
	}

	@Test
	void testGetPropertyDescriptorsOfDerivedClassInstance() throws Exception {
		List<String> propertyNames = collectPropertyNames(PropertyUtils.getPropertyDescriptors(new DerivedClass()));
		assertThat(propertyNames).containsExactly(
			"baseClassStringProperty",
			"class",
			"longPropertyWithPackageAccessSetter",
			"otherStringProperty",
			"sizeFromInterface",
			"stringProperty"
		);
	}

	@Test
	void testGetPropertyDescriptorsOfClassExtendingNonPublicBaseClass() throws Exception {
		List<String> propertyNames = collectPropertyNames(PropertyUtils.getPropertyDescriptors(ClassExtendingNonPublicBaseClass.class));
		assertThat(propertyNames).containsExactly(
			"baseClassProperty",
			"class"
		);
	}

	@Test
	void testGetPropertyDescriptorsOfClassWithDefaultMethods() throws Exception {
		List<String> propertyNames = collectPropertyNames(PropertyUtils.getPropertyDescriptors(ClassWithDefaultMethods.class));
		assertThat(propertyNames).containsExactly(
			"class",
			"id",
			"name"
		);
	}

	@Test
	void testRead_FieldWithoutGetter() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptorByNameOrThrow(TestEntity.class, "fieldWithoutGetter");

		assertThatExceptionOfType(ReflectionRuntimeException.class)
			.isThrownBy(() -> PropertyUtils.read(new TestEntity(), propertyDescriptor))
			.withMessage("Failed to read TestEntity.fieldWithoutGetter")
			.withCauseExactlyInstanceOf(IllegalArgumentException.class)
			.withStackTraceContaining("fieldWithoutGetter must be readable");
	}

	@Test
	void testIsReadable() throws Exception {
		OtherTestEntity entity = new OtherTestEntity();
		assertThat(PropertyUtils.isReadable(PropertyUtils.getPropertyDescriptor(entity, OtherTestEntity::getImmutableValue))).isTrue();
		assertThat(PropertyUtils.isReadable(PropertyUtils.getPropertyDescriptor(entity, OtherTestEntity::getChildren))).isTrue();
		assertThat(PropertyUtils.isReadable(PropertyUtils.getPropertyDescriptorByNameOrThrow(TestEntity.class, "fieldWithoutGetter"))).isFalse();
	}

	@Test
	void testIsWritable() throws Exception {
		OtherTestEntity entity = new OtherTestEntity();
		assertThat(PropertyUtils.isWritable(PropertyUtils.getPropertyDescriptor(entity, OtherTestEntity::getImmutableValue))).isFalse();
		assertThat(PropertyUtils.isWritable(PropertyUtils.getPropertyDescriptor(entity, OtherTestEntity::getName))).isTrue();
		assertThat(PropertyUtils.isWritable(PropertyUtils.getPropertyDescriptorByNameOrThrow(TestEntity.class, "fieldWithoutGetter"))).isTrue();
	}

	@Test
	void testIsFullyAccessible() throws Exception {
		OtherTestEntity entity = new OtherTestEntity();
		assertThat(PropertyUtils.isFullyAccessible(PropertyUtils.getPropertyDescriptor(entity, OtherTestEntity::getImmutableValue))).isFalse();
		assertThat(PropertyUtils.isFullyAccessible(PropertyUtils.getPropertyDescriptor(entity, OtherTestEntity::getName))).isTrue();
	}

	@Test
	void testIsDeclaredInClass() throws Exception {
		assertThat(PropertyUtils.isDeclaredInClass(PropertyUtils.getPropertyDescriptor(BaseClass.class, BaseClass::getBaseClassStringProperty), BaseClass.class)).isTrue();
		assertThat(PropertyUtils.isDeclaredInClass(PropertyUtils.getPropertyDescriptor(DerivedClass.class, DerivedClass::getStringProperty), DerivedClass.class)).isTrue();
		assertThat(PropertyUtils.isDeclaredInClass(PropertyUtils.getPropertyDescriptor(DerivedClass.class, DerivedClass::getBaseClassStringProperty), DerivedClass.class)).isFalse();
	}

	@Test
	void testPropertyHasDefaultValue() throws Exception {
		PropertyDescriptor numberProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);
		PropertyDescriptor stringProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getString);
		PropertyDescriptor someObjectProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getSomeObject);
		TestEntity defaultEntity = new TestEntity();
		assertThat(PropertyUtils.hasDefaultValue(defaultEntity, numberProperty)).isTrue();
		assertThat(PropertyUtils.hasDefaultValue(defaultEntity, stringProperty)).isTrue();
		assertThat(PropertyUtils.hasDefaultValue(defaultEntity, someObjectProperty)).isTrue();

		defaultEntity.setNumber(1);
		defaultEntity.setString("");
		defaultEntity.setSomeObject(null);

		assertThat(PropertyUtils.hasDefaultValue(defaultEntity, numberProperty)).isFalse();
		assertThat(PropertyUtils.hasDefaultValue(defaultEntity, stringProperty)).isFalse();
		assertThat(PropertyUtils.hasDefaultValue(defaultEntity, someObjectProperty)).isFalse();
	}

	@Test
	void testIsDefaultValue() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(BaseClass.class, BaseClass::getBaseClassStringProperty);

		assertThatExceptionOfType(ReflectionRuntimeException.class)
			.isThrownBy(() -> PropertyUtils.isDefaultValue(BaseClass.class, propertyDescriptor, null))
			.withMessage("Failed to determine default value for BaseClass.baseClassStringProperty");

		assertThat(PropertyUtils.isDefaultValue(DerivedClass.class, propertyDescriptor, null)).isTrue();
		assertThat(PropertyUtils.isDefaultValue(DerivedClass.class, propertyDescriptor, "")).isFalse();

		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getLargeNumber, 0L)).isTrue();
		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getLargeNumber, 1L)).isFalse();

		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::isActive, false)).isTrue();
		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::isActive, true)).isFalse();

		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getFloatingNumber, 0.0f)).isTrue();
		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getFloatingNumber, -0.0f)).isTrue();
		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getFloatingNumber, 0)).isFalse();
		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getFloatingNumber, Float.NaN)).isFalse();
		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getFloatingNumber, 123f)).isFalse();

		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getDoubleNumber, 0.0)).isTrue();
		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getDoubleNumber, -0.0)).isTrue();
		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getDoubleNumber, 123.0)).isFalse();
		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getDoubleNumber, Double.NaN)).isFalse();
		assertThat(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getDoubleNumber, 0)).isFalse();
	}

	@Test
	void testGetPropertyDescriptorsWithAnnotation() {
		Map<PropertyDescriptor, Nullable> map = PropertyUtils.getPropertyDescriptorsWithAnnotation(TestEntity.class,
			Nullable.class);
		assertThat(map).hasSize(1);
		PropertyDescriptor someObject = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getSomeObject);
		assertThat(map.get(someObject)).isInstanceOf(Nullable.class);

		Map<PropertyDescriptor, Size> sizeProperties = PropertyUtils.getPropertyDescriptorsWithAnnotation(new DerivedClass(), Size.class);
		assertThat(collectPropertyNames(sizeProperties.keySet())).containsExactly("baseClassStringProperty", "otherStringProperty");
	}

	@Test
	void testPropertyDescriptorByNameOrThrow() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptorByNameOrThrow(TestEntity.class, "number");
		assertThat(propertyDescriptor).isEqualTo(PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber));

		propertyDescriptor = PropertyUtils.getPropertyDescriptorByNameOrThrow(new TestEntity(), "number");
		assertThat(propertyDescriptor).isEqualTo(PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber));

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> PropertyUtils.getPropertyDescriptorByNameOrThrow(TestEntity.class, "propertyThatDoesNotExist"))
			.withMessage("Property 'propertyThatDoesNotExist' not found for 'TestEntity'");
	}

	@Test
	void testPropertyDescriptor() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);
		assertThat(propertyDescriptor.getName()).isEqualTo("number");
	}

	@Test
	void testPropertyDescriptorWithAbstractClass() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(BaseClass.class, BaseClass::getBaseClassStringProperty);
		assertThat(propertyDescriptor.getName()).isEqualTo("baseClassStringProperty");
	}

	@Test
	void testPropertyDescriptor_WithoutField() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getPropertyWithoutField);
		assertThat(propertyDescriptor.getName()).isEqualTo("propertyWithoutField");
	}

	@Test
	void testPropertyDescriptor_ClassWithMethodCaptorField() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(ClassWithMethodCaptorField.class, ClassWithMethodCaptorField::get$methodCaptor);
		assertThat(propertyDescriptor.getName()).isEqualTo("$methodCaptor");
		assertThat(propertyDescriptor.getName()).isEqualTo(MethodCaptor.FIELD_NAME);
	}

	@Test
	void testGetMethod_getParent() throws Exception {
		Method method = PropertyUtils.getMethod(DerivedClass.class, BaseClass::getBaseClassStringProperty);
		assertThat(method.getName()).isEqualTo("getBaseClassStringProperty");
		assertThat(method.getDeclaringClass()).isEqualTo(BaseClass.class);
	}

	@Test
	void testGetMethod_getNumber() throws Exception {
		Method method = PropertyUtils.getMethod(TestEntity.class, TestEntity::getNumber);
		assertThat(method.getName()).isEqualTo("getNumber");
		assertThat(method.getDeclaringClass()).isEqualTo(TestEntity.class);
	}

	@Test
	void testGetMethod_getMethodWithoutProperty() throws Exception {
		Method method = PropertyUtils.getMethod(TestEntity.class, TestEntity::getPropertyWithoutField);
		assertThat(method.getName()).isEqualTo("getPropertyWithoutField");
		assertThat(method.getDeclaringClass()).isEqualTo(TestEntity.class);
	}

	@Test
	void testWrite_setPropertiesAlsoFromBaseClass() {
		// given
		String baseClassStringProperty = "base-class-string-property";
		String stringProperty = "string-property";
		DerivedClass derivedClassObject = new DerivedClass();
		assertThat(derivedClassObject.getBaseClassStringProperty()).isNull();
		assertThat(derivedClassObject.getStringProperty()).isNull();

		// when
		PropertyUtils.write(derivedClassObject, "baseClassStringProperty", baseClassStringProperty);
		PropertyUtils.write(derivedClassObject, "stringProperty", stringProperty);

		// then
		assertThat(derivedClassObject.getBaseClassStringProperty()).isEqualTo(baseClassStringProperty);
		assertThat(derivedClassObject.getStringProperty()).isEqualTo(stringProperty);
	}

	@Test
	void testWrite_FieldWithoutSetter() throws Exception {
		OtherTestEntity testEntity = new OtherTestEntity();
		PropertyDescriptor property = PropertyUtils.getPropertyDescriptor(OtherTestEntity.class, OtherTestEntity::getImmutableValue);

		assertThatExceptionOfType(ReflectionRuntimeException.class)
			.isThrownBy(() -> PropertyUtils.write(testEntity, property, "some value"))
			.withMessage("Failed to write OtherTestEntity.immutableValue")
			.withStackTraceContaining("immutableValue is not writable");

		String fieldName = OtherTestEntity.class.getName() + "." + property.getName();

		assertThatExceptionOfType(ReflectionRuntimeException.class)
			.isThrownBy(() -> PropertyUtils.write(testEntity, property, 12345L, true))
			.withMessage("Failed to write OtherTestEntity.immutableValue")
			.withStackTraceContaining("Can not set final java.lang.String field " + fieldName + " to java.lang.Long");

		PropertyUtils.write(testEntity, property, "changed value", true);
		assertThat(testEntity.getImmutableValue()).isEqualTo("changed value");
	}

	@Test
	void testWrite_setFieldWithPackageSetterAccess() {
		// given
		Long value = 128L;
		DerivedClass derivedClassObject = new DerivedClass();
		assertThat(derivedClassObject.getLongPropertyWithPackageAccessSetter()).isNull();
		PropertyDescriptor propertyWithPackageAccessSetter = PropertyUtils.getPropertyDescriptor(derivedClassObject, DerivedClass::getLongPropertyWithPackageAccessSetter);

		// when
		PropertyUtils.write(derivedClassObject, propertyWithPackageAccessSetter, value, true);

		// then
		assertThat(derivedClassObject.getLongPropertyWithPackageAccessSetter()).isEqualTo(value);
	}

	@Test
	void testWritePropertyIfExists() throws Exception {
		TestEntity destination = new TestEntity();
		PropertyUtils.writeIfPropertyExists(destination, PropertyUtils.getPropertyName(TestEntity.class, TestEntity::getString), () -> "some value");
		assertThat(destination.getString()).isEqualTo("some value");

		PropertyUtils.writeIfPropertyExists(destination, "propertyThatDoesNotExist", () -> {
			throw new UnsupportedOperationException();
		});
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter_HappyCase() throws Exception {
		for (int i = 0; i < 100; i++) {
			PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);
			PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getSomeObject);
		}
	}

	@Test
	void testGetPropertyDescriptorByTypedPropertyGetter() throws Exception {
		TypedPropertyGetter<TestEntity, Integer> getter = TestEntity::getNumber;
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(TestEntity.class, getter);
		assertThat(propertyDescriptor.getName()).isEqualTo("number");
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter() throws Exception {
		PropertyGetter<TestEntity> getter = TestEntity::getNumber;
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(TestEntity.class, getter);
		assertThat(propertyDescriptor.getName()).isEqualTo("number");
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter_CallSiteSpecificLambda() throws Exception {
		TypedPropertyGetter<TestEntity, Integer> someGetter = TestEntity::getNumber;
		PropertyGetter<TestEntity> callSiteSpecificLambda = someGetter::get;

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> PropertyUtils.getPropertyDescriptor(TestEntity.class, callSiteSpecificLambda))
			.withMessage(callSiteSpecificLambda + " is call site specific");
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter_FinalMethodCannotBeCaptured() throws Exception {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getClass))
			.withMessage("Method could not be captured. This can happen when no method was invoked or the method is private or final.");
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter_NonGetterMethod() throws Exception {
		Object className = TestEntity.class.getName();

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::doNothing))
			.withMessage("Found no property for public java.lang.Object " + className + ".doNothing() on class " + className);
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter_NoMethodInvocation() throws Exception {
		PropertyGetter<TestEntity> propertyGetter = e -> null;

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> PropertyUtils.getPropertyDescriptor(TestEntity.class, propertyGetter))
			.withMessage("Method could not be captured. This can happen when no method was invoked or the method is private or final.");
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter_NoVisibleDefaultConstructor() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(EntityProtectedNoDefaultConstructor.class, EntityProtectedNoDefaultConstructor::getSomeProperty);
		assertThat(propertyDescriptor.getName()).isEqualTo("someProperty");
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter_DefaultMethod() throws Exception {
		PropertyDescriptor idProperty = PropertyUtils.getPropertyDescriptor(ClassWithDefaultMethods.class, ClassWithDefaultMethods::getId);
		assertThat(idProperty.getName()).isEqualTo("id");

		PropertyDescriptor nameProperty = PropertyUtils.getPropertyDescriptor(ClassWithDefaultMethods.class, ClassWithDefaultMethods::getName);
		assertThat(nameProperty.getName()).isEqualTo("name");
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter_DefaultMethod_Subclass() throws Exception {
		PropertyDescriptor idProperty = PropertyUtils.getPropertyDescriptor(SubclassOfClassWithDefaultMethods.class, InterfaceWithDefaultMethods::getId);
		assertThat(idProperty.getName()).isEqualTo("id");

		PropertyDescriptor nameProperty = PropertyUtils.getPropertyDescriptor(SubclassOfClassWithDefaultMethods.class, InterfaceWithDefaultMethods::getName);
		assertThat(nameProperty.getName()).isEqualTo("name");
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter_DefaultMethod_SubInterface() throws Exception {
		PropertyDescriptor idProperty = PropertyUtils.getPropertyDescriptor(SubClassOfInterfaceWithDefaultMethods.class, InterfaceWithDefaultMethods::getId);
		assertThat(idProperty.getName()).isEqualTo("id");

		PropertyDescriptor nameProperty = PropertyUtils.getPropertyDescriptor(SubClassOfInterfaceWithDefaultMethods.class, InterfaceWithDefaultMethods::getName);
		assertThat(nameProperty.getName()).isEqualTo("name");
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter_DefaultMethod_ClassWithInheritedDefaultMethods() throws Exception {
		PropertyDescriptor idProperty = PropertyUtils.getPropertyDescriptor(ClassWithInheritedDefaultMethods.class, ClassWithInheritedDefaultMethods::getId);
		assertThat(idProperty.getName()).isEqualTo("id");

		PropertyDescriptor nameProperty = PropertyUtils.getPropertyDescriptor(ClassWithInheritedDefaultMethods.class, ClassWithInheritedDefaultMethods::getName);
		assertThat(nameProperty.getName()).isEqualTo("name");
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter_FinalClass() throws Exception {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> PropertyUtils.getPropertyDescriptor(FinalClass.class, FinalClass::getSomeProperty))
			.withMessage("Cannot subclass primitive, array or final types: " + FinalClass.class);
	}

	@Test
	void testGetPropertyDescriptorByName_FinalClass() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptorByNameOrThrow(FinalClass.class, "someProperty");
		assertThat(propertyDescriptor.getName()).isEqualTo("someProperty");
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter_ClassExtendingNonPublicBaseClass() throws Exception {
		assertThatExceptionOfType(ReflectionRuntimeException.class)
			.isThrownBy(ClassExtendingNonPublicBaseClass::getPropertyDescriptor)
			.withMessageMatching("Failed to create proxy on class .+?")
			.withCauseExactlyInstanceOf(IllegalAccessError.class)
			.withStackTraceContaining("to access class de.cronn.reflection.util.testclasses.NonPublicBaseClass" +
				" from " + ClassExtendingNonPublicBaseClass.class);
	}

	@Test
	void testGetPropertyDescriptorByPropertyGetter_ClassExtendingClassThatExtendsNonPublicBaseClass() throws Exception {
		assertThatExceptionOfType(ReflectionRuntimeException.class)
			.isThrownBy(ClassExtendingClassThatExtendsNonPublicBaseClass::getPropertyDescriptor)
			.withMessageMatching("Failed to create proxy on class .+?")
			.withCauseExactlyInstanceOf(IllegalAccessError.class)
			.withStackTraceContaining("to access class de.cronn.reflection.util.testclasses.NonPublicBaseClass" +
				" from " + ClassExtendingClassThatExtendsNonPublicBaseClass.class);
	}

	@Test
	void testGetPropertyDescriptorByField() throws Exception {
		Field field = TestEntity.class.getDeclaredField("someObject");
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptorByField(TestEntity.class, field);
		assertThat(PropertyUtils.getQualifiedPropertyName(TestEntity.class, propertyDescriptor)).isEqualTo("TestEntity.someObject");
	}

	@Test
	void testGetAnnotationOfProperty() throws Exception {
		Nullable relationship = PropertyUtils.getAnnotationOfProperty(TestEntity.class, TestEntity::getSomeObject, Nullable.class);
		assertThat(relationship).isNotNull();

		Size size = PropertyUtils.getAnnotationOfProperty(TestEntity.class, TestEntity::getSomeObject, Size.class);
		assertThat(size).isNull();

		Size numberSize = PropertyUtils.getAnnotationOfProperty(TestEntity.class, TestEntity::getNumber, Size.class);
		assertThat(numberSize).isNotNull();
		assertThat(numberSize.min()).isEqualTo(10);
		assertThat(numberSize.max()).isEqualTo(20);

		Size stringSize = PropertyUtils.getAnnotationOfProperty(TestEntity.class, TestEntity::getString, Size.class);
		assertThat(stringSize).isNotNull();
		assertThat(stringSize.min()).isEqualTo(0);
		assertThat(stringSize.max()).isEqualTo(1000);

		assertThat(Modifier.isPublic(BaseClass.class.getField(PropertyUtils.getPropertyName(DerivedClass.class, DerivedClass::getOtherStringProperty)).getModifiers())).isTrue();
		Size otherStringSize = PropertyUtils.getAnnotationOfProperty(DerivedClass.class, DerivedClass::getOtherStringProperty, Size.class);
		assertThat(otherStringSize).isNotNull();
		assertThat(otherStringSize.min()).isEqualTo(10);
		assertThat(otherStringSize.max()).isEqualTo(20);

		Size baseClassStringSize = PropertyUtils.getAnnotationOfProperty(DerivedClass.class, BaseClass::getBaseClassStringProperty, Size.class);
		assertThat(baseClassStringSize).isNotNull();
		assertThat(baseClassStringSize.max()).isEqualTo(30);

		Size interfaceStringSize = PropertyUtils.getAnnotationOfProperty(BaseInterface.class, BaseInterface::getSizeFromInterface, Size.class);
		assertThat(interfaceStringSize).isNotNull();
		assertThat(interfaceStringSize.max()).isEqualTo(40);

		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getString);
		assertThat(PropertyUtils.getAnnotationOfProperty(new TestEntity(), propertyDescriptor, Size.class)).isNotNull();
	}

	@Test
	void testGetAnnotationOfProperty_AnnotationOnSetter() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getFieldWithAnnotationOnSetter);
		assertThat(PropertyUtils.getAnnotationOfProperty(new TestEntity(), propertyDescriptor, Size.class)).isNotNull();
	}

	@Test
	void testGetAnnotationOfProperty_AnnotationOnDefaultMethod() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(ClassWithDefaultMethods.class, ClassWithDefaultMethods::getName);
		assertThat(PropertyUtils.getAnnotationOfProperty(ClassWithDefaultMethods.class, propertyDescriptor, Size.class)).isNotNull();
	}

	@Test
	void testHasAnnotationOfProperty() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getSomeObject);
		assertThat(PropertyUtils.hasAnnotationOfProperty(TestEntity.class, propertyDescriptor, Nullable.class)).isTrue();
		assertThat(PropertyUtils.hasAnnotationOfProperty(TestEntity.class, propertyDescriptor, Size.class)).isFalse();
	}

	@Test
	void testGetPropertyName() throws Exception {
		assertThat(PropertyUtils.getPropertyName(TestEntity.class, TestEntity::getSomeObject)).isEqualTo("someObject");
		assertThat(PropertyUtils.getPropertyName(TestEntity.class, TestEntity::getPropertyWithoutField)).isEqualTo("propertyWithoutField");
		assertThat(PropertyUtils.getPropertyName(new TestEntity(), TestEntity::getSomeObject)).isEqualTo("someObject");
		assertThat(PropertyUtils.getPropertyName(new TestEntity(), TestEntity::getPropertyWithoutField)).isEqualTo("propertyWithoutField");
	}

	@Test
	void testQualifiedPropertyName() throws Exception {
		assertThat(PropertyUtils.getQualifiedPropertyName(TestEntity.class, TestEntity::getNumber)).isEqualTo("TestEntity.number");
		assertThat(PropertyUtils.getQualifiedPropertyName(TestEntity.class, TestEntity::getPropertyWithoutField)).isEqualTo("TestEntity.propertyWithoutField");

		assertThat(PropertyUtils.getQualifiedPropertyName(new TestEntity(), TestEntity::getNumber)).isEqualTo("TestEntity.number");

		PropertyDescriptor numberProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);
		assertThat(PropertyUtils.getQualifiedPropertyName(TestEntity.class, numberProperty)).isEqualTo("TestEntity.number");
		assertThat(PropertyUtils.getQualifiedPropertyName(new TestEntity(), numberProperty)).isEqualTo("TestEntity.number");

		Field numberField = getTestEntityField(numberProperty);
		assertThat(PropertyUtils.getQualifiedPropertyName(new TestEntity(), numberField)).isEqualTo("TestEntity.number");
	}

	@Test
	void testReadDirectly() throws Exception {
		TestEntity entity = new TestEntity();
		entity.setNumber(23);
		TestEntity entityMock = Mockito.spy(entity);

		PropertyDescriptor numberProperty = PropertyUtils.getPropertyDescriptor(entityMock, TestEntity::getNumber);
		Object value = PropertyUtils.readDirectly(entityMock, numberProperty);
		assertThat(value).isEqualTo(23);

		Mockito.verifyNoMoreInteractions(entityMock);
	}

	@Test
	void testReadDirectly_FieldWithoutGetter() throws Exception {
		TestEntity testEntity = new TestEntity();
		testEntity.setFieldWithoutGetter("some value");
		PropertyDescriptor property = PropertyUtils.getPropertyDescriptorByNameOrThrow(TestEntity.class, "fieldWithoutGetter");
		Object value = PropertyUtils.readDirectly(testEntity, property);
		assertThat(value).isEqualTo("some value");
	}

	@Test
	void testReadDirectly_Name() throws Exception {
		TestEntity testEntity = new TestEntity();

		PropertyUtils.writeDirectly(testEntity, "fieldWithoutGetter", "new value");
		Object value = PropertyUtils.readDirectly(testEntity, "fieldWithoutGetter");
		assertThat(value).isEqualTo("new value");
	}

	@Test
	void testReadDirectly_PropertyWithoutField() throws Exception {
		TestEntity testEntity = new TestEntity();
		PropertyDescriptor property = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getPropertyWithoutField);

		assertThatExceptionOfType(ReflectionRuntimeException.class)
			.isThrownBy(() -> PropertyUtils.readDirectly(testEntity, property))
			.withMessage("Failed to read TestEntity.propertyWithoutField");
	}

	@Test
	void testReadDirectly_AccessibilityAfterDirectReadIsRestored() throws Exception {
		TestEntity testEntity = new TestEntity();
		PropertyDescriptor property = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);

		Field declaredField = getTestEntityField(property);
		assertThat(declaredField.isAccessible()).isFalse();

		PropertyUtils.readDirectly(testEntity, declaredField);

		assertThat(declaredField.isAccessible()).isFalse();

		declaredField.setAccessible(true);
		assertThat(declaredField.isAccessible()).isTrue();

		PropertyUtils.readDirectly(testEntity, declaredField);
		assertThat(declaredField.isAccessible()).isTrue();
	}

	@Test
	void testWriteDirectly_PropertyWithoutField() throws Exception {
		TestEntity testEntity = new TestEntity();
		PropertyDescriptor property = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getPropertyWithoutField);

		assertThatExceptionOfType(ReflectionRuntimeException.class)
			.isThrownBy(() -> PropertyUtils.writeDirectly(testEntity, property, "some value"))
			.withMessage("Failed to write TestEntity.propertyWithoutField");
	}

	@Test
	void testWriteDirectly_FieldWithoutSetter() throws Exception {
		OtherTestEntity testEntity = new OtherTestEntity();
		PropertyDescriptor property = PropertyUtils.getPropertyDescriptor(OtherTestEntity.class, OtherTestEntity::getImmutableValue);
		PropertyUtils.writeDirectly(testEntity, property, "changed value");
		assertThat(testEntity.getImmutableValue()).isEqualTo("changed value");

		PropertyUtils.writeDirectly(testEntity, property.getName(), "changed again");
		assertThat(testEntity.getImmutableValue()).isEqualTo("changed again");
	}

	@Test
	void testWriteDirectly_WrongType() throws Exception {
		OtherTestEntity testEntity = new OtherTestEntity();
		PropertyDescriptor property = PropertyUtils.getPropertyDescriptor(OtherTestEntity.class, OtherTestEntity::getImmutableValue);
		String fieldName = OtherTestEntity.class.getName() + "." + property.getName();

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> PropertyUtils.writeDirectly(testEntity, property, 12345L))
			.withMessage("Can not set final java.lang.String field " + fieldName + " to java.lang.Long");
	}

	@Test
	void testWriteDirectly_ProxyClass() throws Exception {
		TestEntity testEntity = new TestEntity();
		Class<?> proxyClass = new ByteBuddy()
			.subclass(testEntity.getClass())
			.defineField("$delegate", TestEntity.class)
			.make()
			.load(getClass().getClassLoader())
			.getLoaded();
		Object proxy = ClassUtils.createNewInstance(proxyClass);

		PropertyUtils.writeDirectly(proxy, "$delegate", testEntity);
	}

	@Test
	void testWriteDirectly_Null() throws Exception {
		Field field = TestEntity.class.getDeclaredField("number");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> PropertyUtils.writeDirectly(null, field, "some value"))
			.withMessage("Destination must not be null");
	}

	@Test
	void testReadIfPropertyExists() throws Exception {
		// given
		TestEntity entity = new TestEntity();
		entity.setNumber(123);

		// when
		Object number = PropertyUtils.readIfPropertyExists(entity, "number");
		Object foobar = PropertyUtils.readIfPropertyExists(entity, "foobar");

		// then
		assertThat(number).isEqualTo(123);
		assertThat(foobar).isNull();
	}

	@Test
	void testReadProperty() throws Exception {
		TestEntity entity = new TestEntity();
		entity.setString("some-value");

		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(entity, TestEntity::getString);
		assertThat(PropertyUtils.readProperty(entity, propertyDescriptor, String.class)).isEqualTo("some-value");
		assertThat(PropertyUtils.readProperty(entity, propertyDescriptor, Object.class)).isEqualTo("some-value");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> PropertyUtils.readProperty(entity, propertyDescriptor, Number.class))
			.withMessage(TestEntity.class + ".string is of type " + String.class + " but " + Number.class + " is expected");
	}

	@Test
	void testHasProperty() throws Exception {
		assertThat(PropertyUtils.hasProperty(TestEntity.class, "number")).isTrue();
		assertThat(PropertyUtils.hasProperty(TestEntity.class, "nonExistentProperty")).isFalse();

		assertThat(PropertyUtils.hasProperty(new TestEntity(), "number")).isTrue();
		assertThat(PropertyUtils.hasProperty(new TestEntity(), "nonExistentProperty")).isFalse();
	}

	@Test
	void testIsCollectionType() throws Exception {
		assertThat(PropertyUtils.isCollectionType(PropertyUtils.getPropertyDescriptor(OtherTestEntity.class, OtherTestEntity::getChildren))).isTrue();
		assertThat(PropertyUtils.isCollectionType(PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber))).isFalse();

		assertThat(PropertyUtils.isNotCollectionType(PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber))).isTrue();
		assertThat(PropertyUtils.isNotCollectionType(PropertyUtils.getPropertyDescriptor(OtherTestEntity.class, OtherTestEntity::getChildren))).isFalse();
	}

	@Test
	void testCopyNonDefaultValues() throws Exception {
		TestEntity source = new TestEntity();
		source.setNumber(12);

		TestEntity mock = PropertyUtils.copyNonDefaultValues(source, Mockito.mock(TestEntity.class));

		Mockito.verify(mock).setNumber(12);
		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	void testCopyNonDefaultValuesWithExclusion() throws Exception {
		TestEntity source = new TestEntity();
		source.setNumber(12);
		source.setString("some string");
		source.setSomeObject(new Object());
		source.setFieldWithAnnotationOnSetter("value");

		TestEntity mock = PropertyUtils.copyNonDefaultValues(source, Mockito.mock(TestEntity.class),
			PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getString),
			PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getSomeObject));

		Mockito.verify(mock).setNumber(12);
		Mockito.verify(mock).setFieldWithAnnotationOnSetter("value");
		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	void testCopyNonDefaultValues_NoNonDefaults() throws Exception {
		TestEntity mock = Mockito.mock(TestEntity.class);

		PropertyUtils.copyNonDefaultValues(new TestEntity(), mock);

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	void testCopyValue() throws Exception {
		TestEntity source = new TestEntity();
		source.setNumber(25);

		TestEntity destination = Mockito.mock(TestEntity.class);

		PropertyDescriptor numberProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);
		Object copiedValue = PropertyUtils.copyValue(source, destination, numberProperty);

		assertThat(copiedValue).isEqualTo(25);

		Mockito.verify(destination).setNumber(25);
		Mockito.verifyNoMoreInteractions(destination);
	}

	@Test
	void testHasSameValue() throws Exception {
		TestEntity one = new TestEntity();
		one.setNumber(25);

		TestEntity other = new TestEntity();

		PropertyDescriptor numberProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);
		assertThat(PropertyUtils.hasSameValue(one, other, numberProperty)).isFalse();

		other.setNumber(25);
		assertThat(PropertyUtils.hasSameValue(one, other, numberProperty)).isTrue();

		PropertyDescriptor someObjectProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getSomeObject);
		assertThat(PropertyUtils.hasSameValue(one, other, someObjectProperty)).isTrue();

		one.setSomeObject(null);
		assertThat(PropertyUtils.hasSameValue(one, other, someObjectProperty)).isFalse();

		other.setSomeObject(null);
		assertThat(PropertyUtils.hasSameValue(one, other, someObjectProperty)).isTrue();
	}

	@Test
	void testHasDifferentValue() throws Exception {
		TestEntity one = new TestEntity();
		one.setString("foo");

		TestEntity other = new TestEntity();

		PropertyDescriptor numberProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getString);
		assertThat(PropertyUtils.hasDifferentValue(one, other, numberProperty)).isTrue();

		other.setString("bar");
		assertThat(PropertyUtils.hasDifferentValue(one, other, numberProperty)).isTrue();

		other.setString("foo");
		assertThat(PropertyUtils.hasDifferentValue(one, other, numberProperty)).isFalse();
	}

	@Test
	void testRead_ExceptionInGetter() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getPropertyWithExceptionInGetter);

		assertThatExceptionOfType(ReflectionRuntimeException.class)
			.isThrownBy(() -> PropertyUtils.read(new TestEntity(), propertyDescriptor))
			.withMessage("Failed to read TestEntity.propertyWithExceptionInGetter")
			.withRootCauseExactlyInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void testReadForced_FieldWithoutGetter() throws Exception {
		TestEntity testEntity = new TestEntity();
		testEntity.setFieldWithoutGetter("some value");
		PropertyDescriptor property = PropertyUtils.getPropertyDescriptorByNameOrThrow(TestEntity.class, "fieldWithoutGetter");
		Object value = PropertyUtils.read(testEntity, property, true);
		assertThat(value).isEqualTo("some value");
	}

	@Test
	void testFindMethodByGetter_Annotation() throws Exception {
		TypedPropertyGetter<TestAnnotation, ?> propertyGetter = TestAnnotation::someProperty;
		Method method = PropertyUtils.findMethodByGetter(TestAnnotation.class, propertyGetter);
		assertThat(method).isNotNull();
		assertThat(method.getName()).isEqualTo("someProperty");
	}

	@Test
	@Timeout(30)
	void testConcurrentlyCreateProxyClasses() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(4);
		try {
			CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);
			for (int i = 0; i < 4; i++) {
				completionService.submit(() -> {
					for (int r = 0; r < 100; r++) {
						PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);
						PropertyUtils.clearCache();
					}
					return null;
				});
			}
			for (int i = 0; i < 4; i++) {
				completionService.take().get();
			}
		} finally {
			executorService.shutdown();
		}
	}

	@Test
	void testGetDefaultValueObject() throws Exception {
		assertThat(PropertyUtils.getDefaultValueObject(Object.class)).isNull();
		assertThat(PropertyUtils.getDefaultValueObject(String.class)).isNull();
		assertThat(PropertyUtils.getDefaultValueObject(Boolean.class)).isNull();
		assertThat(PropertyUtils.getDefaultValueObject(Long.class)).isNull();
		assertThat(PropertyUtils.getDefaultValueObject(Integer.class)).isNull();
		assertThat(PropertyUtils.getDefaultValueObject(Number.class)).isNull();

		assertThat(PropertyUtils.getDefaultValueObject(byte.class)).isEqualTo((byte) 0);
		assertThat(PropertyUtils.getDefaultValueObject(char.class)).isEqualTo('\0');
		assertThat(PropertyUtils.getDefaultValueObject(short.class)).isEqualTo((short) 0);
		assertThat(PropertyUtils.getDefaultValueObject(int.class)).isEqualTo(0);
		assertThat(PropertyUtils.getDefaultValueObject(long.class)).isEqualTo(0L);
		assertThat(PropertyUtils.getDefaultValueObject(float.class)).isEqualTo(0.0f);
		assertThat(PropertyUtils.getDefaultValueObject(double.class)).isEqualTo(0.0);
		assertThat(PropertyUtils.getDefaultValueObject(boolean.class)).isEqualTo(false);
		assertThat(PropertyUtils.getDefaultValueObject(void.class)).isNull();
	}

	@Test
	void testProxyIsCached() throws Exception {
		Class<? extends TestEntity> proxy1 = PropertyUtils.getCache(TestEntity.class).getMethodCapturingProxy();
		Class<? extends TestEntity> proxy2 = PropertyUtils.getCache(TestEntity.class).getMethodCapturingProxy();
		assertThat(proxy1).isSameAs(proxy2);

		PropertyUtils.clearCache();

		Class<? extends TestEntity> proxy3 = PropertyUtils.getCache(TestEntity.class).getMethodCapturingProxy();
		assertThat(proxy1).isNotSameAs(proxy3);
	}

	private static List<String> collectPropertyNames(Collection<PropertyDescriptor> propertyDescriptors) {
		return propertyDescriptors.stream()
			.map(PropertyDescriptor::getName)
			.collect(Collectors.toList());
	}

	private static Field getTestEntityField(PropertyDescriptor property) throws Exception {
		return TestEntity.class.getDeclaredField(property.getName());
	}

}
