package de.cronn.reflection.util.immutable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method that does not modify its object, so that {@link ImmutableProxy} delegates it to
 * the original object instead of rejecting it. Getters need no annotation. The annotation is also
 * recognized on an overridden method of a superclass or interface.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ReadOnly {

  /**
   * Whether the return value is wrapped in an immutable proxy. Set to {@code false} if the method
   * returns a value that the caller is allowed to modify, such as a copy.
   */
  boolean proxyReturnValue() default true;
}
