package de.cronn.reflection.util.immutable.collection;

import static de.cronn.reflection.util.immutable.SoftImmutableProxy.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import org.jetbrains.annotations.NotNull;

public class DeepImmutableList<E> extends DeepImmutableCollection<E> implements List<E> {

	private static final long serialVersionUID = 1L;

	private static final String IMMUTABLE_MESSAGE = "This list is immutable";

	private final List<E> listDelegate;

	public DeepImmutableList(List<E> list) {
		this(list, SOFT_IMMUTABLE_DEFAULT);
	}

	public DeepImmutableList(List<E> list, boolean softImmutable) {
		super(list, IMMUTABLE_MESSAGE, softImmutable);
		this.listDelegate = list;
	}

	public static <T> DeepImmutableList<T> of(T elements) {
		return new DeepImmutableList<>(Collections.singletonList(elements));
	}

	public static <T> DeepImmutableList<T> of(T e1, T e2) {
		return new DeepImmutableList<>(Arrays.asList(e1, e2));
	}

	public static <T> DeepImmutableList<T> of(T e1, T e2, T e3) {
		return new DeepImmutableList<>(Arrays.asList(e1, e2, e3));
	}

	@Override
	public E get(int index) {
		E element = listDelegate.get(index);
		return getImmutableElement(element);
	}

	@Override
	public int indexOf(Object o) {
		return listDelegate.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return listDelegate.lastIndexOf(o);
	}

	@NotNull
	@Override
	public ListIterator<E> listIterator() {
		return new ImmutableListIterator<>(this, listDelegate.listIterator());
	}

	@NotNull
	@Override
	public ListIterator<E> listIterator(int index) {
		return new ImmutableListIterator<>(this, listDelegate.listIterator(index));
	}

	@NotNull
	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return new DeepImmutableList<>(listDelegate.subList(fromIndex, toIndex));
	}

	@Override
	public E set(int index, E element) {
		throw new UnsupportedOperationException(IMMUTABLE_MESSAGE);
	}

	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException(IMMUTABLE_MESSAGE);
	}

	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException(IMMUTABLE_MESSAGE);
	}

	@Override
	public boolean addAll(int index, @NotNull Collection<? extends E> c) {
		throw new UnsupportedOperationException(IMMUTABLE_MESSAGE);
	}

	@Override
	public void sort(Comparator<? super E> c) {
		throw new UnsupportedOperationException(IMMUTABLE_MESSAGE);
	}

}
