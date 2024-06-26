package de.cronn.reflection.util.immutable.collection;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import de.cronn.reflection.util.immutable.Immutable;
import de.cronn.reflection.util.immutable.ImmutableProxy;

public class DeepImmutableCollection<E> extends AbstractCollection<E> implements Collection<E>, Immutable, Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final String immutableMessage;

	private final Collection<E> delegate;

	private final Map<E, E> immutableProxyCache = new IdentityHashMap<>();

	public DeepImmutableCollection(Collection<E> delegate) {
		this(delegate, "This collection is immutable");
	}

	DeepImmutableCollection(Collection<E> delegate, String immutableMessage) {
		this.delegate = Objects.requireNonNull(delegate);
		this.immutableMessage = immutableMessage;
	}

	E getImmutableElement(E element) {
		return immutableProxyCache.computeIfAbsent(element, this::createImmutableElement);
	}

	E createImmutableElement(E value) {
		return ImmutableProxy.create(value);
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return delegate.contains(o);
	}

	@NotNull
	@Override
	public Iterator<E> iterator() {
		return new ImmutableIterator<>(this, delegate.iterator(), immutableMessage);
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		return delegate.containsAll(c);
	}

	@Override
	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean add(E t) {
		throw new UnsupportedOperationException(immutableMessage);
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException(immutableMessage);
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends E> c) {
		throw new UnsupportedOperationException(immutableMessage);
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		throw new UnsupportedOperationException(immutableMessage);
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		throw new UnsupportedOperationException(immutableMessage);
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(immutableMessage);
	}

}
