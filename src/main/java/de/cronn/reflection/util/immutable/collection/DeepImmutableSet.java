package de.cronn.reflection.util.immutable.collection;

import static de.cronn.reflection.util.immutable.SoftImmutableProxy.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import de.cronn.reflection.util.immutable.ImmutableProxy;

public class DeepImmutableSet<E> extends DeepImmutableCollection<E> implements Set<E> {

	private static final long serialVersionUID = 1L;

	public DeepImmutableSet(Set<E> set) {
		this(set, SOFT_IMMUTABLE_DEFAULT);
	}

	public DeepImmutableSet(Set<E> set, boolean softImmutable) {
		super(set, "This set is immutable", softImmutable);
	}

	public DeepImmutableSet<E> withSoftImmutable(boolean softImmutable) {
		return new DeepImmutableSet<>(ImmutableProxy.unwrap(this), softImmutable);
	}

	public static <T> DeepImmutableSet<T> of(T element) {
		return new DeepImmutableSet<>(Collections.singleton(element));
	}

	public static <T> DeepImmutableSet<T> of(T e1, T e2) {
		return new DeepImmutableSet<>(new LinkedHashSet<>(Arrays.asList(e1, e2)));
	}

	public static <T> DeepImmutableSet<T> of(T e1, T e2, T e3) {
		return new DeepImmutableSet<>(new LinkedHashSet<>(Arrays.asList(e1, e2, e3)));
	}

}
