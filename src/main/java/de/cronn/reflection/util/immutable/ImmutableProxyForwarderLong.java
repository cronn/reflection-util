package de.cronn.reflection.util.immutable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;

public final class ImmutableProxyForwarderLong {

	private ImmutableProxyForwarderLong() {
	}

	public static Long forward(@Origin Method method,
							   @FieldValue(ImmutableProxySupport.DELEGATE_FIELD_NAME) Object delegate,
							   @AllArguments Object[] args) throws InvocationTargetException, IllegalAccessException {
		return (Long) method.invoke(delegate, args);
	}

}
