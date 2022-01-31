package de.cronn.reflection.util.immutable;

public enum ImmutableProxyOption {
	/**
	 * Allow cloning of records if at least one of the record components is not clearly an immutable type.
	 *
	 * Examples:
	 *
	 * <code>
	 * record RecordWithPrimitives(int a, int b) {}
	 *
	 * record RecordWithList(List<String> list) {}
	 * </code>
	 *
	 * For records of type RecordWithPrimitives, no cloning is required since it only contains clearly immutable components.
	 * However, when creating an immutable proxy for an instance of RecordWithList,
	 * we need to clone the record and wrap the list component into an immutable proxy itself.
	 */
	ALLOW_CLONING_RECORDS
}
