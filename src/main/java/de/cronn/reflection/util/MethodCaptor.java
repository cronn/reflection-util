package de.cronn.reflection.util;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

public class MethodCaptor {

	static final String FIELD_NAME = "$methodCaptor";

	private final AtomicReference<Method> capturedMethod = new AtomicReference<>();

	void capture(Method method) {
		Method existing = capturedMethod.getAndSet(method);
		Assert.isNull(existing, () -> String.format("Method already captured: %s called twice?", existing));
	}

	Method getCapturedMethod() {
		Method method = capturedMethod.get();
		Assert.notNull(method, () -> "Method could not be captured." +
									 " This can happen when no method was invoked or the method is final or non-public.");
		return method;
	}

	@RuntimeType
	public static Object intercept(@Origin Method method, @FieldValue(FIELD_NAME) MethodCaptor methodCaptor) {
		methodCaptor.capture(method);
		return PropertyUtils.getDefaultValueObject(method.getReturnType());
	}

	static <T> Class<? extends T> createProxyClass(Class<T> beanClass) {
		try (DynamicType.Unloaded<T> unloadedType = new ByteBuddy()
			.subclass(beanClass, ConstructorStrategy.Default.NO_CONSTRUCTORS)
			.defineField(MethodCaptor.FIELD_NAME, MethodCaptor.class, Visibility.PRIVATE)
			.method(isMethod()
				.and(takesArguments(0))
				.and(not(isDeclaredBy(Object.class))))
			.intercept(MethodDelegation.to(MethodCaptor.class))
			.make()) {
			return unloadedType
				.load(PropertyUtils.class.getClassLoader())
				.getLoaded();
		} catch (IllegalAccessError e) {
			throw new ReflectionRuntimeException("Failed to create proxy on " + beanClass, e);
		}
	}

}
