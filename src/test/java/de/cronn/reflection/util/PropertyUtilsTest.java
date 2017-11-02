package de.cronn.reflection.util;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.Size;

import org.junit.Test;
import org.mockito.Mockito;

import de.cronn.reflection.util.testclasses.BaseClass;
import de.cronn.reflection.util.testclasses.BaseInterface;
import de.cronn.reflection.util.testclasses.ClassWithPrimitives;
import de.cronn.reflection.util.testclasses.DerivedClass;
import de.cronn.reflection.util.testclasses.OtherTestEntity;
import de.cronn.reflection.util.testclasses.TestEntity;

public class PropertyUtilsTest {

	@Test
	public void testGetPropertyDescriptors() throws Exception {
		List<String> testEntityPropertyNames = collectPropertyNames(PropertyUtils.getPropertyDescriptors(TestEntity.class));
		assertThat(testEntityPropertyNames, contains(
			"class",
			"fieldWithoutGetter",
			"number",
			"propertyWithoutField",
			"someObject",
			"string"
		));

		List<String> derivedClassPropertyNames = collectPropertyNames(PropertyUtils.getPropertyDescriptors(new DerivedClass()));
		assertThat(derivedClassPropertyNames, contains(
			"baseClassStringProperty",
			"class",
			"longPropertyWithPackageAccessSetter",
			"otherStringProperty",
			"sizeFromInterface",
			"stringProperty"
		));
	}

	@Test
	public void testRead_FieldWithoutGetter() throws Exception {
		try {
			PropertyUtils.read(new TestEntity(), PropertyUtils.getPropertyDescriptorByNameOrThrow(TestEntity.class, "fieldWithoutGetter"));
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("fieldWithoutGetter must be readable", e.getMessage());
		}
	}

	@Test
	public void testIsReadable() throws Exception {
		OtherTestEntity entity = new OtherTestEntity();
		assertTrue(PropertyUtils.isReadable(PropertyUtils.getPropertyDescriptor(entity, OtherTestEntity::getImmutableValue)));
		assertTrue(PropertyUtils.isReadable(PropertyUtils.getPropertyDescriptor(entity, OtherTestEntity::getChildren)));
		assertFalse(PropertyUtils.isReadable(PropertyUtils.getPropertyDescriptorByNameOrThrow(TestEntity.class, "fieldWithoutGetter")));
	}

	@Test
	public void testIsWritable() throws Exception {
		OtherTestEntity entity = new OtherTestEntity();
		assertFalse(PropertyUtils.isWritable(PropertyUtils.getPropertyDescriptor(entity, OtherTestEntity::getImmutableValue)));
		assertTrue(PropertyUtils.isWritable(PropertyUtils.getPropertyDescriptor(entity, OtherTestEntity::getName)));
		assertTrue(PropertyUtils.isWritable(PropertyUtils.getPropertyDescriptorByNameOrThrow(TestEntity.class, "fieldWithoutGetter")));
	}

	@Test
	public void testIsFullyAccessible() throws Exception {
		OtherTestEntity entity = new OtherTestEntity();
		assertFalse(PropertyUtils.isFullyAccessible(PropertyUtils.getPropertyDescriptor(entity, OtherTestEntity::getImmutableValue)));
		assertTrue(PropertyUtils.isFullyAccessible(PropertyUtils.getPropertyDescriptor(entity, OtherTestEntity::getName)));
	}

	@Test
	public void testIsDeclaredInClass() throws Exception {
		assertTrue(PropertyUtils.isDeclaredInClass(PropertyUtils.getPropertyDescriptor(BaseClass.class, BaseClass::getBaseClassStringProperty), BaseClass.class));
		assertTrue(PropertyUtils.isDeclaredInClass(PropertyUtils.getPropertyDescriptor(DerivedClass.class, DerivedClass::getStringProperty), DerivedClass.class));
		assertFalse(PropertyUtils.isDeclaredInClass(PropertyUtils.getPropertyDescriptor(DerivedClass.class, DerivedClass::getBaseClassStringProperty), DerivedClass.class));
	}

	@Test
	public void testPropertyHasDefaultValue() throws Exception {
		PropertyDescriptor numberProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);
		PropertyDescriptor stringProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getString);
		PropertyDescriptor someObjectProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getSomeObject);
		TestEntity defaultEntity = new TestEntity();
		assertTrue(PropertyUtils.hasDefaultValue(defaultEntity, numberProperty));
		assertTrue(PropertyUtils.hasDefaultValue(defaultEntity, stringProperty));
		assertTrue(PropertyUtils.hasDefaultValue(defaultEntity, someObjectProperty));

		defaultEntity.setNumber(1);
		defaultEntity.setString("");
		defaultEntity.setSomeObject(null);

		assertFalse(PropertyUtils.hasDefaultValue(defaultEntity, numberProperty));
		assertFalse(PropertyUtils.hasDefaultValue(defaultEntity, stringProperty));
		assertFalse(PropertyUtils.hasDefaultValue(defaultEntity, someObjectProperty));
	}

	@Test
	public void testIsDefaultValue() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(BaseClass.class, BaseClass::getBaseClassStringProperty);
		try {
			PropertyUtils.isDefaultValue(BaseClass.class, propertyDescriptor, null);
			fail("ReflectionRuntimeException expected");
		} catch (ReflectionRuntimeException e) {
			assertThat(e.getMessage(), is("Failed to determine default value for BaseClass.baseClassStringProperty"));
		}

		assertTrue(PropertyUtils.isDefaultValue(DerivedClass.class, propertyDescriptor, null));
		assertFalse(PropertyUtils.isDefaultValue(DerivedClass.class, propertyDescriptor, ""));

		assertTrue(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getLargeNumber, 0L));
		assertFalse(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getLargeNumber, 1L));
		assertTrue(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::isActive, false));
		assertFalse(PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::isActive, true));

		try {
			PropertyUtils.isDefaultValue(ClassWithPrimitives.class, ClassWithPrimitives::getFloatingNumber, null);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("Unhandled primitive type: float", e.getMessage());
		}
	}

	@Test
	public void testGetPropertyDescriptorsWithAnnotation() {
		Map<PropertyDescriptor, Nullable> map = PropertyUtils.getPropertyDescriptorsWithAnnotation(TestEntity.class,
			Nullable.class);
		assertThat(map.keySet(), hasSize(1));
		PropertyDescriptor someObject = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getSomeObject);
		assertThat(map.get(someObject), instanceOf(Nullable.class));

		Map<PropertyDescriptor, Size> sizeProperties = PropertyUtils.getPropertyDescriptorsWithAnnotation(new DerivedClass(), Size.class);
		assertThat(collectPropertyNames(sizeProperties.keySet()), contains("baseClassStringProperty", "otherStringProperty"));
	}

	@Test
	public void testPropertyDescriptorByNameOrThrow() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptorByNameOrThrow(TestEntity.class, "number");
		assertThat(propertyDescriptor, is(PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber)));

		propertyDescriptor = PropertyUtils.getPropertyDescriptorByNameOrThrow(new TestEntity(), "number");
		assertThat(propertyDescriptor, is(PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber)));

		try {
			PropertyUtils.getPropertyDescriptorByNameOrThrow(TestEntity.class, "propertyThatDoesNotExist");
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("Property 'propertyThatDoesNotExist' not found for 'TestEntity'", e.getMessage());
		}
	}

	@Test
	public void testPropertyDescriptor() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);
		assertEquals("number", propertyDescriptor.getName());
	}

	@Test
	public void testPropertyDescriptorWithAbstractClass() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(BaseClass.class, BaseClass::getBaseClassStringProperty);
		assertEquals("baseClassStringProperty", propertyDescriptor.getName());
	}

	@Test
	public void testPropertyDescriptor_WithoutField() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getPropertyWithoutField);
		assertEquals("propertyWithoutField", propertyDescriptor.getName());
	}

	@Test
	public void testGetMethod_getParent() throws Exception {
		Method method = PropertyUtils.getMethod(DerivedClass.class, BaseClass::getBaseClassStringProperty);
		assertEquals("getBaseClassStringProperty", method.getName());
		assertEquals(BaseClass.class, method.getDeclaringClass());
	}

	@Test
	public void testGetMethod_getNumber() throws Exception {
		Method method = PropertyUtils.getMethod(TestEntity.class, TestEntity::getNumber);
		assertEquals("getNumber", method.getName());
		assertEquals(TestEntity.class, method.getDeclaringClass());
	}

	@Test
	public void testGetMethod_getMethodWithoutProperty() throws Exception {
		Method method = PropertyUtils.getMethod(TestEntity.class, TestEntity::getPropertyWithoutField);
		assertEquals("getPropertyWithoutField", method.getName());
		assertEquals(TestEntity.class, method.getDeclaringClass());
	}

	@Test
	public void testWrite_setPropertiesAlsoFromBaseClass() {
		// given
		String baseClassStringProperty = "base-class-string-property";
		String stringProperty = "string-property";
		DerivedClass derivedClassObject = new DerivedClass();
		assertThat(derivedClassObject.getBaseClassStringProperty(), nullValue());
		assertThat(derivedClassObject.getStringProperty(), nullValue());

		// when
		PropertyUtils.write(derivedClassObject, "baseClassStringProperty", baseClassStringProperty);
		PropertyUtils.write(derivedClassObject, "stringProperty", stringProperty);

		// then
		assertThat(derivedClassObject.getBaseClassStringProperty(), equalTo(baseClassStringProperty));
		assertThat(derivedClassObject.getStringProperty(), equalTo(stringProperty));
	}

	@Test
	public void testWrite_setFieldWithPackageSetterAccess() {
		// given
		Long value = 128L;
		DerivedClass derivedClassObject = new DerivedClass();
		assertThat(derivedClassObject.getLongPropertyWithPackageAccessSetter(), nullValue());

		PropertyDescriptor propertyWithPackageAccessSetter = PropertyUtils.getPropertyDescriptor(derivedClassObject, DerivedClass::getLongPropertyWithPackageAccessSetter);

		// when
		PropertyUtils.write(derivedClassObject, propertyWithPackageAccessSetter, value, true);

		// then
		assertThat(derivedClassObject.getLongPropertyWithPackageAccessSetter(), equalTo(value));
	}

	@Test
	public void testWritePropertyIfExists() throws Exception {
		TestEntity destination = new TestEntity();
		PropertyUtils.writeIfPropertyExists(destination, PropertyUtils.getPropertyName(TestEntity.class, TestEntity::getString), () -> "some value");
		assertEquals("some value", destination.getString());

		PropertyUtils.writeIfPropertyExists(destination, "propertyThatDoesNotExist", () -> {
			throw new UnsupportedOperationException();
		});
	}

	@Test
	public void testGetPropertyDescriptorByPropertyGetter_HappyCase() throws Exception {
		for (int i = 0; i < 100; i++) {
			PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);
			PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getSomeObject);
		}
	}

	@Test
	public void testGetPropertyDescriptorByPropertyGetter_ManyLambdas() throws Exception {
		for (int i = 0; i < 2000; i++) {
			boolean useNumber = i % 2 == 0;
			PropertyGetter<TestEntity> callSiteSpecificLambda = e -> useNumber ? e.getNumber() : e.getSomeObject();
			PropertyUtils.getPropertyDescriptor(TestEntity.class, callSiteSpecificLambda);
		}
	}

	@Test
	public void testGetPropertyDescriptorByPropertyGetter_FinalMethodCannotBeCaptured() throws Exception {
		try {
			PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getClass);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("Method could not be captured. This can happen when no method was invoked or the method is private or final.", e.getMessage());
		}
	}

	@Test
	public void testGetPropertyDescriptorByPropertyGetter_NonGetterMethod() throws Exception {
		try {
			PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::doNothing);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			Object className = TestEntity.class.getName();
			assertEquals("Found no property for public java.lang.Object " + className + ".doNothing() on class class " + className, e.getMessage());
		}
	}

	@Test
	public void testGetPropertyDescriptorByPropertyGetter_NoMethodInvocation() throws Exception {
		PropertyGetter<TestEntity> propertyGetter = e -> null;
		try {
			PropertyUtils.getPropertyDescriptor(TestEntity.class, propertyGetter);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("Method could not be captured. This can happen when no method was invoked or the method is private or final.", e.getMessage());
		}
	}

	@Test
	public void testGetPropertyDescriptorByField() throws Exception {
		Field field = TestEntity.class.getDeclaredField("someObject");
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptorByField(TestEntity.class, field);
		assertEquals("TestEntity.someObject", PropertyUtils.getQualifiedPropertyName(TestEntity.class, propertyDescriptor));
	}

	@Test
	public void testGetAnnotationOfProperty() throws Exception {
		Nullable relationship = PropertyUtils.getAnnotationOfProperty(TestEntity.class, TestEntity::getSomeObject, Nullable.class);
		assertNotNull(relationship);

		Size size = PropertyUtils.getAnnotationOfProperty(TestEntity.class, TestEntity::getSomeObject, Size.class);
		assertNull(size);

		Size numberSize = PropertyUtils.getAnnotationOfProperty(TestEntity.class, TestEntity::getNumber, Size.class);
		assertNotNull(numberSize);
		assertThat(numberSize.min(), is(10));
		assertThat(numberSize.max(), is(20));

		Size stringSize = PropertyUtils.getAnnotationOfProperty(TestEntity.class, TestEntity::getString, Size.class);
		assertNotNull(stringSize);
		assertThat(stringSize.min(), is(0));
		assertThat(stringSize.max(), is(1000));

		assertTrue(Modifier.isPublic(BaseClass.class.getField(PropertyUtils.getPropertyName(DerivedClass.class, DerivedClass::getOtherStringProperty)).getModifiers()));
		Size otherStringSize = PropertyUtils.getAnnotationOfProperty(DerivedClass.class, DerivedClass::getOtherStringProperty, Size.class);
		assertNotNull(otherStringSize);
		assertThat(otherStringSize.min(), is(10));
		assertThat(otherStringSize.max(), is(20));

		Size baseClassStringSize = PropertyUtils.getAnnotationOfProperty(DerivedClass.class, BaseClass::getBaseClassStringProperty, Size.class);
		assertNotNull(baseClassStringSize);
		assertThat(baseClassStringSize.max(), is(30));

		Size interfaceStringSize = PropertyUtils.getAnnotationOfProperty(BaseInterface.class, BaseInterface::getSizeFromInterface, Size.class);
		assertNotNull(interfaceStringSize);
		assertThat(interfaceStringSize.max(), is(40));

		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getString);
		assertNotNull(PropertyUtils.getAnnotationOfProperty(new TestEntity(), propertyDescriptor, Size.class));
	}

	@Test
	public void testHasAnnotationOfProperty() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getSomeObject);
		assertTrue(PropertyUtils.hasAnnotationOfProperty(TestEntity.class, propertyDescriptor, Nullable.class));
		assertFalse(PropertyUtils.hasAnnotationOfProperty(TestEntity.class, propertyDescriptor, Size.class));
	}

	@Test
	public void testGetPropertyName() throws Exception {
		assertEquals("someObject", PropertyUtils.getPropertyName(TestEntity.class, TestEntity::getSomeObject));
		assertEquals("propertyWithoutField", PropertyUtils.getPropertyName(TestEntity.class, TestEntity::getPropertyWithoutField));
		assertEquals("someObject", PropertyUtils.getPropertyName(new TestEntity(), TestEntity::getSomeObject));
		assertEquals("propertyWithoutField", PropertyUtils.getPropertyName(new TestEntity(), TestEntity::getPropertyWithoutField));
	}

	@Test
	public void testQualifiedPropertyName() throws Exception {
		assertEquals("TestEntity.number", PropertyUtils.getQualifiedPropertyName(TestEntity.class, TestEntity::getNumber));
		assertEquals("TestEntity.propertyWithoutField", PropertyUtils.getQualifiedPropertyName(TestEntity.class, TestEntity::getPropertyWithoutField));

		assertEquals("TestEntity.number", PropertyUtils.getQualifiedPropertyName(new TestEntity(), TestEntity::getNumber));

		PropertyDescriptor numberProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);
		assertEquals("TestEntity.number", PropertyUtils.getQualifiedPropertyName(TestEntity.class, numberProperty));
		assertEquals("TestEntity.number", PropertyUtils.getQualifiedPropertyName(new TestEntity(), numberProperty));
	}

	@Test
	public void testReadDirectly() throws Exception {
		TestEntity entity = new TestEntity();
		entity.setNumber(23);
		TestEntity entityMock = Mockito.spy(entity);

		PropertyDescriptor numberProperty = PropertyUtils.getPropertyDescriptor(entityMock, TestEntity::getNumber);
		Object value = PropertyUtils.readDirectly(entityMock, numberProperty);
		assertEquals(23, value);

		Mockito.verifyNoMoreInteractions(entityMock);
	}

	@Test
	public void testReadDirectly_FieldWithoutGetter() throws Exception {
		TestEntity testEntity = new TestEntity();
		testEntity.setFieldWithoutGetter("some value");
		PropertyDescriptor property = PropertyUtils.getPropertyDescriptorByNameOrThrow(TestEntity.class, "fieldWithoutGetter");
		Object value = PropertyUtils.readDirectly(testEntity, property);
		assertEquals("some value", value);
	}

	@Test
	public void testReadDirectly_PropertyWithoutField() throws Exception {
		TestEntity testEntity = new TestEntity();
		PropertyDescriptor property = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getPropertyWithoutField);
		try {
			PropertyUtils.readDirectly(testEntity, property);
			fail("ReflectionRuntimeException expected");
		} catch (ReflectionRuntimeException e) {
			assertEquals("Failed to read TestEntity.propertyWithoutField", e.getMessage());
		}
	}

	@Test
	public void testWriteDirectly_PropertyWithoutField() throws Exception {
		TestEntity testEntity = new TestEntity();
		PropertyDescriptor property = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getPropertyWithoutField);
		try {
			PropertyUtils.writeDirectly(testEntity, property, "some value");
			fail("ReflectionRuntimeException expected");
		} catch (ReflectionRuntimeException e) {
			assertEquals("Failed to write TestEntity.propertyWithoutField", e.getMessage());
		}
	}

	@Test
	public void testReadIfPropertyExists() throws Exception {
		// given
		TestEntity entity = new TestEntity();
		entity.setNumber(123);

		// when
		Object number = PropertyUtils.readIfPropertyExists(entity, "number");
		Object foobar = PropertyUtils.readIfPropertyExists(entity, "foobar");

		// then
		assertThat(number, is(123));
		assertNull(foobar);
	}

	@Test
	public void testReadProperty() throws Exception {
		TestEntity entity = new TestEntity();
		entity.setString("some-value");

		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(entity, TestEntity::getString);
		assertEquals("some-value", PropertyUtils.readProperty(entity, propertyDescriptor, String.class));
		assertEquals("some-value", PropertyUtils.readProperty(entity, propertyDescriptor, Object.class));

		try {
			PropertyUtils.readProperty(entity, propertyDescriptor, Number.class);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals(TestEntity.class + ".string is of type " + String.class + " but " + Number.class + " is expected", e.getMessage());
		}
	}

	@Test
	public void testHasProperty() throws Exception {
		assertTrue(PropertyUtils.hasProperty(TestEntity.class, "number"));
		assertFalse(PropertyUtils.hasProperty(TestEntity.class, "nonExistentProperty"));

		assertTrue(PropertyUtils.hasProperty(new TestEntity(), "number"));
		assertFalse(PropertyUtils.hasProperty(new TestEntity(), "nonExistentProperty"));
	}

	@Test
	public void testIsCollectionType() throws Exception {
		assertTrue(PropertyUtils.isCollectionType(PropertyUtils.getPropertyDescriptor(OtherTestEntity.class, OtherTestEntity::getChildren)));
		assertFalse(PropertyUtils.isCollectionType(PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber)));

		assertTrue(PropertyUtils.isNotCollectionType(PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber)));
		assertFalse(PropertyUtils.isNotCollectionType(PropertyUtils.getPropertyDescriptor(OtherTestEntity.class, OtherTestEntity::getChildren)));
	}

	@Test
	public void testCopyNonDefaultValues() throws Exception {
		TestEntity source = new TestEntity();
		source.setNumber(12);

		TestEntity mock = PropertyUtils.copyNonDefaultValues(source, Mockito.mock(TestEntity.class));

		Mockito.verify(mock).setNumber(12);
		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testCopyNonDefaultValues_NoNonDefaults() throws Exception {
		TestEntity mock = Mockito.mock(TestEntity.class);

		PropertyUtils.copyNonDefaultValues(new TestEntity(), mock);

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testCopyValue() throws Exception {
		TestEntity source = new TestEntity();
		source.setNumber(25);

		TestEntity destination = Mockito.mock(TestEntity.class);

		PropertyDescriptor numberProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);
		PropertyUtils.copyValue(source, destination, numberProperty);

		Mockito.verify(destination).setNumber(25);
		Mockito.verifyNoMoreInteractions(destination);
	}

	@Test
	public void testHasSameValue() throws Exception {
		TestEntity one = new TestEntity();
		one.setNumber(25);

		TestEntity other = new TestEntity();

		PropertyDescriptor numberProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getNumber);
		assertFalse(PropertyUtils.hasSameValue(one, other, numberProperty));

		other.setNumber(25);
		assertTrue(PropertyUtils.hasSameValue(one, other, numberProperty));

		PropertyDescriptor someObjectProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getSomeObject);
		assertTrue(PropertyUtils.hasSameValue(one, other, someObjectProperty));

		one.setSomeObject(null);
		assertFalse(PropertyUtils.hasSameValue(one, other, someObjectProperty));

		other.setSomeObject(null);
		assertTrue(PropertyUtils.hasSameValue(one, other, someObjectProperty));
	}

	@Test
	public void testHasDifferentValue() throws Exception {
		TestEntity one = new TestEntity();
		one.setString("foo");

		TestEntity other = new TestEntity();

		PropertyDescriptor numberProperty = PropertyUtils.getPropertyDescriptor(TestEntity.class, TestEntity::getString);
		assertTrue(PropertyUtils.hasDifferentValue(one, other, numberProperty));

		other.setString("bar");
		assertTrue(PropertyUtils.hasDifferentValue(one, other, numberProperty));

		other.setString("foo");
		assertFalse(PropertyUtils.hasDifferentValue(one, other, numberProperty));
	}

	private static List<String> collectPropertyNames(Collection<PropertyDescriptor> propertyDescriptors) {
		return propertyDescriptors.stream()
			.map(PropertyDescriptor::getName)
			.collect(Collectors.toList());
	}

}
