package de.cronn.reflection.util.immutable.collection;

import java.util.ListIterator;

class ImmutableListIterator<E> implements ListIterator<E> {

	private final DeepImmutableList<E> list;
	private final ListIterator<E> delegate;

	ImmutableListIterator(DeepImmutableList<E> list, ListIterator<E> delegate) {
		this.list = list;
		this.delegate = delegate;
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public E next() {
		E element = delegate.next();
		return list.getImmutableElement(element);
	}

	@Override
	public boolean hasPrevious() {
		return delegate.hasPrevious();
	}

	@Override
	public E previous() {
		E element = delegate.previous();
		return list.getImmutableElement(element);
	}

	@Override
	public int nextIndex() {
		return delegate.nextIndex();
	}

	@Override
	public int previousIndex() {
		return delegate.previousIndex();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("This list is immutable");
	}

	@Override
	public void set(E t) {
		throw new UnsupportedOperationException("This list is immutable");
	}

	@Override
	public void add(E t) {
		throw new UnsupportedOperationException("This list is immutable");
	}
}
