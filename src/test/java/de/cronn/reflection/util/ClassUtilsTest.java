package de.cronn.reflection.util;

import static de.cronn.reflection.util.TestUtils.*;
import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.cronn.reflection.util.immutable.ReadOnly;
import de.cronn.reflection.util.testclasses.BaseInterface;
import de.cronn.reflection.util.testclasses.ClassWithInheritedDefaultMethods;
import de.cronn.reflection.util.testclasses.DerivedClass;
import de.cronn.reflection.util.testclasses.EntityProtectedConstructor;
import de.cronn.reflection.util.testclasses.EntityProtectedNoDefaultConstructor;
import de.cronn.reflection.util.testclasses.FindMethodByArgumentTypesTestCaseClass;
import de.cronn.reflection.util.testclasses.FindMethodByArgumentTypesTestCaseSubclass;
import de.cronn.reflection.util.testclasses.OtherClass;
import de.cronn.reflection.util.testclasses.SomeClass;
import de.cronn.reflection.util.testclasses.SomeTestInterface;
import de.cronn.reflection.util.testclasses.SubclassOfClassWithDefaultMethods;
import de.cronn.reflection.util.testclasses.TestEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import javassist.util.proxy.ProxyFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;

public class ClassUtilsTest {

	private static final Class<?> SOME_TEST_INTERFACE_CLASS = SomeTestInterface.class;

	@Test
	void testConstructor() throws Exception {
		assertThatConstructorIsPrivate(ClassUtils.class);
	}

	@Test
	void testGetRealClass() throws Exception {
		assertThat(ClassUtils.getRealClass(new TestEntity())).isSameAs(TestEntity.class);
		assertThat(ClassUtils.getRealClass(TestEntity.class)).isSameAs(TestEntity.class);
		assertThat(ClassUtils.getRealClass(createJdkProxy(SomeTestInterface.class))).isSameAs(SomeTestInterface.class);
		assertThat(ClassUtils.getRealClass(createJdkProxy(SomeTestInterface.class).getClass())).isSameAs(SomeTestInterface.class);
		assertThat(ClassUtils.getRealClass(createByteBuddyProxy(new TestEntity()))).isSameAs(TestEntity.class);
		assertThat(ClassUtils.getRealClass(createJavassistProxy(new TestEntity()))).isSameAs(TestEntity.class);
		assertThat(ClassUtils.getRealClass(createByteBuddyProxy(createJavassistProxy(new TestEntity())))).isSameAs(TestEntity.class);
		assertThat(ClassUtils.getRealClass(Long.valueOf(16))).isSameAs(Long.class);

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> ClassUtils.getRealClass(createJdkProxy(SomeTestInterface.class, BaseInterface.class)))
			.withMessage("Unexpected number of interfaces: 2");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> {
				Object entity = TestEntity.class;
				ClassUtils.getRealClass(entity);
			})
			.withMessage("The provided object is already a class: class de.cronn.reflection.util.testclasses.TestEntity." +
						 " You probably want to call ClassUtils.getRealClass(Class) instead.");
	}

	private static Stream<Arguments> testMatchesWellKnownProxyClassPatternParams() {
		return Stream.of(
			Arguments.of(Object.class.getName(), false),
			Arguments.of(String.class.getName(), false),
			Arguments.of("my.package.SomeClass", false),

			Arguments.of("my.package.SomeClass$$proxy", true),
			Arguments.of("my.package.SomeClass$ByteBuddy$abcdef", true),
			Arguments.of("my.package.SomeClass$HibernateProxy$abcdef", true)
		);
	}

	@ParameterizedTest
	@MethodSource("testMatchesWellKnownProxyClassPatternParams")
	void testMatchesWellKnownProxyClassPattern(String given, boolean expected) throws Exception {
		assertThat(ClassUtils.matchesWellKnownProxyClassNamePattern(given)).isEqualTo(expected);
	}

	@Test
	void testCreateNewInstanceLikeOfProxy() throws Exception {
		Object sourceEntity = new TestEntity();
		Object proxy = createJavassistProxy(sourceEntity);

		Object newInstance = ClassUtils.createNewInstanceLike(proxy);
		assertThat(newInstance.getClass()).isSameAs(TestEntity.class);
	}

	@Test
	void testCreateNewInstanceLike_Null() throws Exception {
		Object instance = ClassUtils.createNewInstanceLike(null);
		assertThat(instance).isNull();
	}

	@Test
	void testCreateNewInstanceLikeProtectedNoArgConstructor() throws Exception {
		Object sourceEntity = EntityProtectedConstructor.newEntity();
		Object actual = ClassUtils.createNewInstanceLike(sourceEntity);
		assertThat(actual).isInstanceOf(EntityProtectedConstructor.class);
	}

	@Test
	void testCreateNewInstanceLikeProtectedConstructor() throws Exception {
		Object sourceEntity = EntityProtectedNoDefaultConstructor.newEntity();

		assertThatExceptionOfType(ReflectionRuntimeException.class)
			.isThrownBy(() -> ClassUtils.createNewInstanceLike(sourceEntity))
			.withCauseExactlyInstanceOf(NoSuchMethodException.class)
			.withMessage("Failed to construct an instance of " + EntityProtectedNoDefaultConstructor.class);
	}

	@Test
	public void testGetVoidMethod() throws Exception {
		Method voidMethod = ClassUtils.getVoidMethod(ClassUtilsTest.class, ClassUtilsTest::testGetVoidMethod);
		assertThat(voidMethod.getName()).isEqualTo("testGetVoidMethod");
	}

	@Test
	void testGetVoidMethod_CallSiteSpecificLambda() throws Exception {
		VoidMethod<ClassUtilsTest> lambda = ClassUtilsTest::testGetVoidMethod;
		VoidMethod<ClassUtilsTest> callSiteSpecificLambda = lambda::invoke;

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> ClassUtils.getVoidMethod(ClassUtilsTest.class, callSiteSpecificLambda))
			.withMessage(callSiteSpecificLambda + " is call site specific");
	}

	@Test
	void testGetVoidMethod_lambdaWithException() throws Exception {
		assertThatExceptionOfType(ReflectionRuntimeException.class)
			.isThrownBy(() -> ClassUtils.getVoidMethod(ClassUtilsTest.class, bean -> {
				throw new IllegalStateException("some exception");
			}))
			.withRootCauseExactlyInstanceOf(IllegalStateException.class)
			.withMessage("java.lang.IllegalStateException: some exception");
	}

	@Test
	void testGetVoidMethod_lambdaThatIsNoRealMethod() throws Exception {
		VoidMethod<TestEntity> lambda = bean -> {
		};
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> ClassUtils.getMethod(TestEntity.class, lambda))
			.withMessage("Method could not be captured. This can happen when no method was invoked or the method is final or non-public.");
	}

	@Test
	public void testGetVoidMethodName() throws Exception {
		String voidMethodName = ClassUtils.getVoidMethodName(ClassUtilsTest.class, ClassUtilsTest::testGetVoidMethod);
		assertThat(voidMethodName).isEqualTo("testGetVoidMethod");

		String methodName = ClassUtils.getVoidMethodName(new ClassUtilsTest(), ClassUtilsTest::testGetVoidMethod);
		assertThat(methodName).isEqualTo("testGetVoidMethod");

		assertThat(ClassUtils.getVoidMethodName(SomeTestInterface.class, SomeTestInterface::doOtherWork)).isEqualTo("doOtherWork");
	}

	@Test
	void testGetVoidMethodName_AnonymousClass() throws Exception {
		SomeClass bean = new SomeClass() {
		};

		assertThatExceptionOfType(ReflectionRuntimeException.class)
			.isThrownBy(() -> ClassUtils.getVoidMethodName(bean, SomeClass::doOtherWork))
			.withMessageMatching("Failed to create proxy on class .+?")
			.withCauseExactlyInstanceOf(IllegalAccessError.class)
			.withStackTraceContaining("cannot access its superclass");
	}

	@Test
	void testGetMethodName() throws Exception {
		assertThat(ClassUtils.getMethodName(TestEntity.class, TestEntity::getNumber)).isEqualTo("getNumber");
		assertThat(ClassUtils.getMethodName(new TestEntity(), TestEntity::getNumber)).isEqualTo("getNumber");
		assertThat(ClassUtils.getMethodName(new TestEntity(), TestEntity::getSomePath)).isEqualTo("getSomePath");
		assertThat(ClassUtils.getMethodName(new TestEntity(), TestEntity::getString)).isEqualTo("getString");
		assertThat(ClassUtils.getMethodName(new TestEntity(), TestEntity::doNothing)).isEqualTo("doNothing");
		assertThat(ClassUtils.getMethodName(new TestEntity(), TestEntity::clear)).isEqualTo("clear");
		assertThat(ClassUtils.getMethodName(new TestEntity(), TestEntity::countSomeList)).isEqualTo("countSomeList");
		assertThat(ClassUtils.getMethodName(TestEntity.class, TestEntity::countSomeList)).isEqualTo("countSomeList");
		assertThat(ClassUtils.getMethodName(SomeClass.class, SomeClass::doOtherWork)).isEqualTo("doOtherWork");
		assertThat(ClassUtils.getMethodName(new SomeClass(), SomeClass::doOtherWork)).isEqualTo("doOtherWork");
	}

	@Test
	void testGetMethod() throws Exception {
		assertThat(ClassUtils.getMethod(TestEntity.class, TestEntity::getNumber).getName()).isEqualTo("getNumber");
		assertThat(ClassUtils.getMethod(new TestEntity(), TestEntity::getNumber).getName()).isEqualTo("getNumber");
		assertThat(ClassUtils.getMethod(new TestEntity(), TestEntity::getSomePath).getName()).isEqualTo("getSomePath");
		assertThat(ClassUtils.getMethod(new TestEntity(), TestEntity::doNothing).getName()).isEqualTo("doNothing");
		assertThat(ClassUtils.getMethod(new TestEntity(), TestEntity::countSomeList).getName()).isEqualTo("countSomeList");
		assertThat(ClassUtils.getMethod(TestEntity.class, TestEntity::countSomeList).getName()).isEqualTo("countSomeList");
	}

	@Test
	void testGetMethod_lambdaWithException() throws Exception {
		PropertyGetter<TestEntity> getter = bean -> {
			throw new IllegalStateException("some exception");
		};
		assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> ClassUtils.getMethod(TestEntity.class, getter))
			.withMessage("some exception");
	}

	@Test
	void testGetMethod_lambdaThatIsNoRealGetter() throws Exception {
		PropertyGetter<TestEntity> getter = bean -> {
			return null;
		};
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> ClassUtils.getMethod(TestEntity.class, getter))
			.withMessage("Method could not be captured. This can happen when no method was invoked or the method is final or non-public.");
	}

	@Test
	void testGetMethod_CallSiteSpecificLambda() throws Exception {
		PropertyGetter<TestEntity> lambda = TestEntity::getNumber;
		PropertyGetter<TestEntity> callSiteSpecificLambda = lambda::get;

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> ClassUtils.getMethod(TestEntity.class, callSiteSpecificLambda))
			.withMessage(callSiteSpecificLambda + " is call site specific");
	}

	@Test
	void testIsProxy() throws Exception {
		Object testObject = new TestEntity();
		assertThat(ClassUtils.isProxy(createJdkProxy(BaseInterface.class))).isTrue();
		assertThat(ClassUtils.isProxy(createByteBuddyProxy(testObject))).isTrue();
		assertThat(ClassUtils.isProxy(createJavassistProxy(testObject))).isTrue();
		assertThat(ClassUtils.isProxy(testObject)).isFalse();
		assertThat(ClassUtils.isProxy("some string")).isFalse();
		assertThat(ClassUtils.isProxy(null)).isFalse();
	}

	@Test
	void testIsProxyClass() throws Exception {
		Object testObject = new TestEntity();
		assertThat(ClassUtils.isProxyClass(createJdkProxy(BaseInterface.class).getClass())).isTrue();
		assertThat(ClassUtils.isProxyClass(createByteBuddyProxy(testObject).getClass())).isTrue();
		assertThat(ClassUtils.isProxyClass(createJavassistProxy(testObject).getClass())).isTrue();
		assertThat(ClassUtils.isProxyClass(PropertyUtils.getCache(TestEntity.class).getMethodCapturingProxy())).isTrue();
		assertThat(ClassUtils.isProxyClass(testObject.getClass())).isFalse();
		assertThat(ClassUtils.isProxyClass(String.class)).isFalse();
		assertThat(ClassUtils.isProxyClass(null)).isFalse();
	}

	@Test
	void testIsProxyClass_Hibernate() throws Exception {
		HibernateProxyTestUtil.runWithHibernateProxy(personProxy -> {
			assertThat(personProxy).isInstanceOf(HibernateProxy.class);
			assertThat(personProxy.getClass().getSimpleName()).endsWith("$Person$HibernateProxy");

			assertThat(ClassUtils.isProxy(personProxy)).isTrue();
			assertThat(ClassUtils.isProxyClass(personProxy.getClass())).isTrue();
		});
	}

	@Test
	void testHasMethodWithSameSignature_happyPath_shouldMatchMethodSignature_whenReturnTypeAndNameAndParametersAreEqual() throws Exception {
		Method targetMethod = findMethod(SomeClass.class, "doWork", int.class);

		assertThat(ClassUtils.hasMethodWithSameSignature(SOME_TEST_INTERFACE_CLASS, targetMethod)).isTrue();
	}

	@Test
	void testHasMethodWithSameSignature_shouldNotMatchMethodSignature_whenReturnTypeIsDifferentThanHappyPath() throws Exception {
		Method targetMethod = findMethod(OtherClass.class, "doWork", int.class);

		assertThat(ClassUtils.hasMethodWithSameSignature(SOME_TEST_INTERFACE_CLASS, targetMethod)).isFalse();
	}

	@Test
	void testHasMethodWithSameSignature_shouldNotMatchMethodSignature_whenNameIsDifferentThanHappyPath() throws Exception {
		Method targetMethod = findMethod(SomeClass.class, "doWorkLater", int.class);

		assertThat(ClassUtils.hasMethodWithSameSignature(SOME_TEST_INTERFACE_CLASS, targetMethod)).isFalse();
	}

	@Test
	void testHasMethodWithSameSignature_shouldNotMatchMethodSignature_whenParametersAreDifferentThanHappyPath() throws Exception {
		Method targetMethod = findMethod(SomeClass.class, "doWork", int.class, int.class);

		assertThat(ClassUtils.hasMethodWithSameSignature(SOME_TEST_INTERFACE_CLASS, targetMethod)).isFalse();
	}

	@Test
	void testIsFromPackage() throws Exception {
		assertThat(ClassUtils.isFromPackage(ClassUtilsTest.class, "de.cronn.reflection.util")).isTrue();
		assertThat(ClassUtils.isFromPackage(ClassUtilsTest.class, "de.cronn.reflection")).isFalse();
		assertThat(ClassUtils.isFromPackage(ClassUtilsTest.class, "de.cronn")).isFalse();
		assertThat(ClassUtils.isFromPackage(ClassUtilsTest.class, "non.existing.package")).isFalse();
		assertThat(ClassUtils.isFromPackage(SomeClass.class, "de.cronn.reflection.util.testclasses")).isTrue();
	}

	@Test
	void testFindDeclaredMethodsByArgumentTypes() throws Exception {
		assertThat(ClassUtils.findMethodsByArgumentTypes(FindMethodByArgumentTypesTestCaseClass.class, String.class, Integer.class)).hasSize(2);
		assertThat(ClassUtils.findMethodsByArgumentTypes(FindMethodByArgumentTypesTestCaseSubclass.class, String.class, Integer.class)).hasSize(3);
	}

	@Test
	void testHaveSameSignature() throws Exception {
		Method oneMethod = SomeClass.class.getMethod("doOtherWork");
		Method otherMethod = SomeTestInterface.class.getMethod("doOtherWork");
		Method hashCodeMethod = Object.class.getMethod("hashCode");
		assertThat(ClassUtils.haveSameSignature(otherMethod, otherMethod)).isTrue();
		assertThat(ClassUtils.haveSameSignature(oneMethod, oneMethod)).isTrue();
		assertThat(ClassUtils.haveSameSignature(oneMethod, otherMethod)).isTrue();
		assertThat(ClassUtils.haveSameSignature(oneMethod, hashCodeMethod)).isFalse();
		assertThat(ClassUtils.haveSameSignature(otherMethod, hashCodeMethod)).isFalse();

		Method doWorkWithOneParameter = SomeClass.class.getMethod("doWork", int.class);
		Method doWorkWithOneParameterFromInterface = SomeTestInterface.class.getMethod("doWork", int.class);
		Method doWorkWithTwoParameters = SomeClass.class.getMethod("doWork", int.class, int.class);
		assertThat(ClassUtils.haveSameSignature(doWorkWithOneParameter, doWorkWithTwoParameters)).isFalse();
		assertThat(ClassUtils.haveSameSignature(doWorkWithOneParameter, doWorkWithOneParameterFromInterface)).isTrue();
	}

	@Test
	void testGetAllDeclaredMethods() throws Exception {
		assertThat(withoutJacocoMethods(ClassUtils.getAllDeclaredMethods(BaseInterface.class))).hasSize(1);
		assertThat(withoutJacocoMethods(ClassUtils.getAllDeclaredMethods(SomeClass.class))).hasSize(6);
	}

	@Test
	void testGetAllDeclaredMethodSignatures() throws Exception {
		Set<MethodSignature> methodsOfSomeClass = withoutJacocoMethodSignatures(ClassUtils.getAllDeclaredMethodSignatures(SomeClass.class));
		withoutJacocoMethodSignatures(methodsOfSomeClass);
		assertThat(mapToString(methodsOfSomeClass)).containsExactly(
			"void doOtherWork()",
			"void doWork(int)",
			"void doWork(int, int)",
			"void doWorkLater(int)"
		);

		Set<MethodSignature> methodsOfDerivedClass = withoutJacocoMethodSignatures(ClassUtils.getAllDeclaredMethodSignatures(DerivedClass.class));
		assertThat(mapToString(methodsOfDerivedClass)).containsExactly(
			"java.lang.String getBaseClassStringProperty()",
			"java.lang.Long getLongPropertyWithPackageAccessSetter()",
			"java.lang.String getOtherStringProperty()",
			"java.lang.String getSizeFromInterface()",
			"java.lang.String getStringProperty()",
			"void setBaseClassStringProperty(java.lang.String)",
			"void setLongPropertyWithPackageAccessSetter(java.lang.Long)",
			"void setStringProperty(java.lang.String)"
		);

		assertThat(withoutJacocoMethodSignatures(ClassUtils.getAllDeclaredMethodSignatures(SomeClass.class))).hasSize(4);
	}

	@Test
	void testCreateInstance_AccessibleFlagIsRestored() throws Exception {
		Constructor<EntityProtectedConstructor> constructor = EntityProtectedConstructor.class.getDeclaredConstructor();
		assertThat(constructor.canAccess(null)).isFalse();

		assertThat(ClassUtils.createInstance(constructor)).isNotNull();

		assertThat(constructor.canAccess(null)).isFalse();

		constructor.setAccessible(true);

		assertThat(ClassUtils.createInstance(constructor)).isNotNull();

		assertThat(constructor.canAccess(null)).isTrue();
	}

	@Test
	void testFindAnnotation() throws Exception {
		Method getNumberMethod = ClassUtils.getVoidMethod(TestEntity.class, TestEntity::getNumber);
		assertThat(ClassUtils.findAnnotation(getNumberMethod, ReadOnly.class)).isNull();

		Method setFieldWithAnnotationOnSetter = TestEntity.class.getMethod("setFieldWithAnnotationOnSetter", String.class);
		assertThat(ClassUtils.findAnnotation(setFieldWithAnnotationOnSetter, Size.class)).isNotNull();

		Method asMyself = TestEntity.class.getMethod("asMyself");
		assertThat(ClassUtils.findAnnotation(asMyself, ReadOnly.class)).isNotNull();

		Method countNothing = TestEntity.class.getMethod("countNothing");
		assertThat(ClassUtils.findAnnotation(countNothing, ReadOnly.class)).isNotNull();

		for (Class<?> clazz : Arrays.asList(ClassWithInheritedDefaultMethods.class, SubclassOfClassWithDefaultMethods.class)) {
			Method getName = clazz.getMethod("getName");
			assertThat(ClassUtils.findAnnotation(getName, Size.class)).isNotNull();

			Method getId = clazz.getMethod("getId");
			assertThat(ClassUtils.findAnnotation(getId, NotNull.class)).isNotNull();
		}
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
		try (DynamicType.Unloaded<T> unloadedType = new ByteBuddy()
			.subclass(ClassUtils.getRealClass(object))
			.make()) {
			Class<? extends T> proxyClass = unloadedType
				.load(ClassUtilsTest.class.getClassLoader())
				.getLoaded();
			return ClassUtils.createNewInstance(proxyClass);
		}
	}

	private static Method findMethod(Class<?> aClass, String name, Class<?>... parameterTypes) throws Exception {
		return aClass.getDeclaredMethod(name, parameterTypes);
	}

}
