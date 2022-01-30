package de.cronn.reflection.util.immutable;

public class ClassWithFinalReadOnlyMethods {
	@Override
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public final String toString() {
		return super.toString();
	}

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	@Override
	protected final Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@ReadOnly
	public final int size() {
		return 0;
	}
}
