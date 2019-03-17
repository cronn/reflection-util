package de.cronn.reflection.util.immutable.collection;

import java.util.Iterator;

class ImmutableIterator<E> implements Iterator<E> {

	private final DeepImmutableCollection<E> collection;
	private final Iterator<E> delegate;
	private final String immutableMessage;

	ImmutableIterator(DeepImmutableCollection<E> collection, Iterator<E> delegate, String immutableMessage) {
		this.collection = collection;
		this.delegate = delegate;
		this.immutableMessage = immutableMessage;
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public E next() {
		E element = delegate.next();
		return collection.getImmutableElement(element);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException(immutableMessage);
	}

}
