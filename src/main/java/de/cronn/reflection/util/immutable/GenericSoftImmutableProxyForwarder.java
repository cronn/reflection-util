package de.cronn.reflection.util.immutable;

import static de.cronn.reflection.util.immutable.SoftImmutableProxy.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

public abstract class GenericSoftImmutableProxyForwarder {

	@RuntimeType
	public static Object forward(@Origin Method method,
								 @FieldValue(ImmutableProxySupport.DELEGATE_FIELD_NAME) Object delegate,
								 @AllArguments Object[] args) throws InvocationTargetException, IllegalAccessException {
		return ImmutableProxyForwarderSupport.forward(method, delegate, args, SOFT_IMMUTABLE_ENABLED);
	}

}
