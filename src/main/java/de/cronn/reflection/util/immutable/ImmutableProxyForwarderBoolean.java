package de.cronn.reflection.util.immutable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;

/** Internal: only public because generated proxy classes call it. Not part of the API. */
public final class ImmutableProxyForwarderBoolean {

  private ImmutableProxyForwarderBoolean() {}

  public static Boolean forward(
      @Origin Method method,
      @FieldValue(ImmutableProxy.DELEGATE_FIELD_NAME) Object delegate,
      @AllArguments Object[] args)
      throws InvocationTargetException, IllegalAccessException {
    return (Boolean) method.invoke(delegate, args);
  }
}
