package de.cronn.reflection.util;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import de.cronn.reflection.util.testclasses.BaseInterface;
import de.cronn.reflection.util.testclasses.DerivedClass;
import de.cronn.reflection.util.testclasses.EntityProtectedConstructor;
import de.cronn.reflection.util.testclasses.EntityProtectedNoDefaultConstructor;
import de.cronn.reflection.util.testclasses.FindMethodByArgumentTypesTestCaseClass;
import de.cronn.reflection.util.testclasses.FindMethodByArgumentTypesTestCaseSubclass;
import de.cronn.reflection.util.testclasses.OtherClass;
import de.cronn.reflection.util.testclasses.SomeClass;
import de.cronn.reflection.util.testclasses.SomeTestInterface;
import de.cronn.reflection.util.testclasses.TestEntity;
import javassist.util.proxy.ProxyFactory;
import net.bytebuddy.ByteBuddy;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.FixedValue;

public class ClassUtilsTest {

	private static final Class<?> SOME_TEST_INTERFACE_CLASS = SomeTestInterface.class;

	@Test
	public void testGetRealClass() throws Exception {
		assertSame(TestEntity.class, ClassUtils.getRealClass(new TestEntity()));
		assertSame(SomeTestInterface.class, ClassUtils.getRealClass(createJdkProxy(SomeTestInterface.class)));
		assertSame(TestEntity.class, ClassUtils.getRealClass(createByteBuddyProxy(new TestEntity())));
		assertSame(TestEntity.class, ClassUtils.getRealClass(createJavassistProxy(new TestEntity())));
		assertSame(TestEntity.class, ClassUtils.getRealClass(createCglibProxy(new TestEntity())));
		assertSame(TestEntity.class, ClassUtils.getRealClass(createByteBuddyProxy(createCglibProxy(new TestEntity()))));
		assertSame(TestEntity.class, ClassUtils.getRealClass(createByteBuddyProxy(createJavassistProxy(new TestEntity()))));
		assertSame(TestEntity.class, ClassUtils.getRealClass(createByteBuddyProxy(createJavassistProxy(createCglibProxy(new TestEntity())))));
		assertSame(TestEntity.class, ClassUtils.getRealClass(createCglibProxy(createByteBuddyProxy(new TestEntity()))));
		assertSame(Long.class, ClassUtils.getRealClass(Long.valueOf(16)));

		try {
			ClassUtils.getRealClass(createJdkProxy(SomeTestInterface.class, BaseInterface.class));
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("Unexpected number of interfaces: 2", e.getMessage());
		}
	}

	@Test
	public void testCreateNewInstanceLikeOfProxy() throws Exception {
		Object sourceEntity = new TestEntity();
		Object proxy = createCglibProxy(sourceEntity);

		Object newInstance = ClassUtils.createNewInstanceLike(proxy);
		assertSame(TestEntity.class, newInstance.getClass());
	}

	@Test
	public void testCreateNewInstanceLike_Null() throws Exception {
		Assert.assertNull(ClassUtils.createNewInstanceLike(null));
	}

	@Test
	public void testCreateNewInstanceLikeProtectedNoArgConstructor() throws Exception {
		Object sourceEntity = EntityProtectedConstructor.newEntity();
		Object actual = ClassUtils.createNewInstanceLike(sourceEntity);
		assertThat(actual, Matchers.instanceOf(EntityProtectedConstructor.class));
	}

	@Test
	public void testCreateNewInstanceLikeProtectedConstructor() throws Exception {
		Object sourceEntity = EntityProtectedNoDefaultConstructor.newEntity();

		try {
			ClassUtils.createNewInstanceLike(sourceEntity);
			fail("ReflectionRuntimeException exception expected");
		} catch (ReflectionRuntimeException e) {
			assertThat(e.getCause(), CoreMatchers.instanceOf(NoSuchMethodException.class));
			Object className = EntityProtectedNoDefaultConstructor.class;
			assertEquals("Failed to construct an instance of " + className, e.getMessage());
		}
	}

	@Test
	public void testGetVoidMethod() throws Exception {
		Method voidMethod = ClassUtils.getVoidMethod(ClassUtilsTest.class, ClassUtilsTest::testGetVoidMethod);
		assertThat(voidMethod.getName(), is("testGetVoidMethod"));
	}

	@Test
	public void testGetVoidMethodName() throws Exception {
		String voidMethodName = ClassUtils.getVoidMethodName(ClassUtilsTest.class, ClassUtilsTest::testGetVoidMethod);
		assertThat(voidMethodName, is("testGetVoidMethod"));

		String methodName = ClassUtils.getVoidMethodName(new ClassUtilsTest(), ClassUtilsTest::testGetVoidMethod);
		assertThat(methodName, is("testGetVoidMethod"));

		assertEquals("doOtherWork", ClassUtils.getVoidMethodName(SomeTestInterface.class, SomeTestInterface::doOtherWork));
		assertEquals("doOtherWork", ClassUtils.getVoidMethodName(new SomeClass() {}, SomeClass::doOtherWork));
	}

	@Test
	public void testIsProxy() throws Exception {
		Object testObject = new TestEntity();
		assertTrue(ClassUtils.isProxy(createJdkProxy(BaseInterface.class)));
		assertTrue(ClassUtils.isProxy(createByteBuddyProxy(testObject)));
		assertTrue(ClassUtils.isProxy(createCglibProxy(testObject)));
		assertFalse(ClassUtils.isProxy(testObject));
		assertFalse(ClassUtils.isProxy("some string"));
		assertFalse(ClassUtils.isProxy(null));
	}

	@Test
	public void testIsProxyClass() throws Exception {
		Object testObject = new TestEntity();
		assertTrue(ClassUtils.isProxyClass(createJdkProxy(BaseInterface.class).getClass()));
		assertTrue(ClassUtils.isProxyClass(createByteBuddyProxy(testObject).getClass()));
		assertTrue(ClassUtils.isProxyClass(createCglibProxy(testObject).getClass()));
		assertFalse(ClassUtils.isProxyClass(testObject.getClass()));
		assertFalse(ClassUtils.isProxyClass(String.class));
		assertFalse(ClassUtils.isProxyClass(null));
	}

	@Test
	public void testHasMethodWithSameSignature_happyPath_shouldMatchMethodSignature_whenReturnTypeAndNameAndParametersAreEqual() throws Exception {
		Method targetMethod = findMethod(SomeClass.class, "doWork", int.class);

		assertTrue(ClassUtils.hasMethodWithSameSignature(SOME_TEST_INTERFACE_CLASS, targetMethod));
	}

	@Test
	public void testHasMethodWithSameSignature_shouldNotMatchMethodSignature_whenReturnTypeIsDifferentThanHappyPath() throws Exception {
		Method targetMethod = findMethod(OtherClass.class, "doWork", int.class);

		assertFalse(ClassUtils.hasMethodWithSameSignature(SOME_TEST_INTERFACE_CLASS, targetMethod));
	}

	@Test
	public void testHasMethodWithSameSignature_shouldNotMatchMethodSignature_whenNameIsDifferentThanHappyPath() throws Exception {
		Method targetMethod = findMethod(SomeClass.class, "doWorkLater", int.class);

		assertFalse(ClassUtils.hasMethodWithSameSignature(SOME_TEST_INTERFACE_CLASS, targetMethod));
	}

	@Test
	public void testHasMethodWithSameSignature_shouldNotMatchMethodSignature_whenParametersAreDifferentThanHappyPath() throws Exception {
		Method targetMethod = findMethod(SomeClass.class, "doWork", int.class, int.class);

		assertFalse(ClassUtils.hasMethodWithSameSignature(SOME_TEST_INTERFACE_CLASS, targetMethod));
	}

	@Test
	public void testIsFromPackage() throws Exception {
		assertTrue(ClassUtils.isFromPackage(ClassUtilsTest.class, "de.cronn.reflection.util"));
		assertFalse(ClassUtils.isFromPackage(ClassUtilsTest.class, "de.cronn.reflection"));
		assertFalse(ClassUtils.isFromPackage(ClassUtilsTest.class, "de.cronn"));
		assertFalse(ClassUtils.isFromPackage(ClassUtilsTest.class, "non.existing.package"));
		assertTrue(ClassUtils.isFromPackage(SomeClass.class, "de.cronn.reflection.util.testclasses"));
	}

	@Test
	public void testFindDeclaredMethodsByArgumentTypes() throws Exception {
		assertThat(ClassUtils.findMethodsByArgumentTypes(FindMethodByArgumentTypesTestCaseClass.class, String.class, Integer.class), hasSize(2));
		assertThat(ClassUtils.findMethodsByArgumentTypes(FindMethodByArgumentTypesTestCaseSubclass.class, String.class, Integer.class), hasSize(3));
	}

	@Test
	public void testHaveSameSignature() throws Exception {
		Method oneMethod = SomeClass.class.getMethod("doOtherWork");
		Method otherMethod = SomeTestInterface.class.getMethod("doOtherWork");
		Method hashCodeMethod = Object.class.getMethod("hashCode");
		assertTrue(ClassUtils.haveSameSignature(otherMethod, otherMethod));
		assertTrue(ClassUtils.haveSameSignature(oneMethod, oneMethod));
		assertTrue(ClassUtils.haveSameSignature(oneMethod, otherMethod));
		assertFalse(ClassUtils.haveSameSignature(oneMethod, hashCodeMethod));
		assertFalse(ClassUtils.haveSameSignature(otherMethod, hashCodeMethod));

		Method doWorkWithOneParameter = SomeClass.class.getMethod("doWork", int.class);
		Method doWorkWithOneParameterFromInterface = SomeTestInterface.class.getMethod("doWork", int.class);
		Method doWorkWithTwoParameters = SomeClass.class.getMethod("doWork", int.class, int.class);
		assertFalse(ClassUtils.haveSameSignature(doWorkWithOneParameter, doWorkWithTwoParameters));
		assertTrue(ClassUtils.haveSameSignature(doWorkWithOneParameter, doWorkWithOneParameterFromInterface));
	}

	@Test
	public void testGetAllDeclaredMethods() throws Exception {
		assertThat(withoutJacocoMethods(ClassUtils.getAllDeclaredMethods(BaseInterface.class)), hasSize(1));
		assertThat(withoutJacocoMethods(ClassUtils.getAllDeclaredMethods(SomeClass.class)), hasSize(6));
	}

	@Test
	public void testGetAllDeclaredMethodSignatures() throws Exception {
		Set<MethodSignature> methodsOfSomeClass = withoutJacocoMethodSignatures(ClassUtils.getAllDeclaredMethodSignatures(SomeClass.class));
		withoutJacocoMethodSignatures(methodsOfSomeClass);
		assertThat(mapToString(methodsOfSomeClass), contains(
			"void doOtherWork()",
			"void doWork(int)",
			"void doWork(int, int)",
			"void doWorkLater(int)"
		));

		Set<MethodSignature> methodsOfDerivedClass = withoutJacocoMethodSignatures(ClassUtils.getAllDeclaredMethodSignatures(DerivedClass.class));
		assertThat(mapToString(methodsOfDerivedClass), contains(
			"java.lang.String getBaseClassStringProperty()",
			"java.lang.Long getLongPropertyWithPackageAccessSetter()",
			"java.lang.String getOtherStringProperty()",
			"java.lang.String getSizeFromInterface()",
			"java.lang.String getStringProperty()",
			"void setBaseClassStringProperty(java.lang.String)",
			"void setLongPropertyWithPackageAccessSetter(java.lang.Long)",
			"void setStringProperty(java.lang.String)"
		));

		assertThat(withoutJacocoMethodSignatures(ClassUtils.getAllDeclaredMethodSignatures(SomeClass.class)), hasSize(4));
	}

	private static Set<MethodSignature> withoutJacocoMethodSignatures(Set<MethodSignature> methodSignatures) {
		return methodSignatures.stream()
			.filter(methodSignature -> !methodSignature.getName().contains("jacoco"))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static Set<Method> withoutJacocoMethods(Set<Method> methods) {
		return methods.stream()
			.filter(method -> !method.getName().contains("jacoco"))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static List<String> mapToString(Collection<MethodSignature> methodSignatures) {
		return methodSignatures.stream()
			.map(MethodSignature::toString)
			.collect(Collectors.toList());
	}

	private static <T> T createCglibProxy(T object) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(object.getClass());
		enhancer.setCallback((FixedValue) () -> null);
		@SuppressWarnings("unchecked")
		T proxy = (T) enhancer.create();
		return proxy;
	}

	private static <T> T createJavassistProxy(T object) {
		ProxyFactory factory = new ProxyFactory();
		factory.setSuperclass(object.getClass());
		@SuppressWarnings("unchecked")
		T proxy = (T) ClassUtils.createNewInstance(factory.createClass());
		return proxy;
	}

	private static Object createJdkProxy(Class<?>... interfaces) {
		return Proxy.newProxyInstance(ClassUtilsTest.class.getClassLoader(), interfaces, (p, method, args) -> null);
	}

	private static <T> T createByteBuddyProxy(T object) {
		Class<? extends T> proxyClass = new ByteBuddy()
			.subclass(ClassUtils.getRealClass(object))
			.make()
			.load(ClassUtilsTest.class.getClassLoader())
			.getLoaded();
		return ClassUtils.createNewInstance(proxyClass);
	}

	private static Method findMethod(Class<?> aClass, String name, Class<?>... parameterTypes) throws Exception {
		return aClass.getDeclaredMethod(name, parameterTypes);
	}

}