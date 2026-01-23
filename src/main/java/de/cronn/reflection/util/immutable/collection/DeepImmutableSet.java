package de.cronn.reflection.util.immutable.collection;

import java.io.Serial;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import de.cronn.reflection.util.immutable.ImmutableProxyOption;

public class DeepImmutableSet<E> extends DeepImmutableCollection<E> implements Set<E> {

	@Serial
	private static final long serialVersionUID = 1L;

	public DeepImmutableSet(Set<E> set, ImmutableProxyOption[] options) {
		super(set, options, "This set is immutable");
	}

	public static <T> DeepImmutableSet<T> of(T element, ImmutableProxyOption... options) {
		return new DeepImmutableSet<>(Collections.singleton(element), options);
	}

	public static <T> DeepImmutableSet<T> of(T e1, T e2, ImmutableProxyOption... options) {
		return new DeepImmutableSet<>(new LinkedHashSet<>(Arrays.asList(e1, e2)), options);
	}

	public static <T> DeepImmutableSet<T> of(T e1, T e2, T e3, ImmutableProxyOption... options) {
		return new DeepImmutableSet<>(new LinkedHashSet<>(Arrays.asList(e1, e2, e3)), options);
	}

}
