package de.cronn.reflection.util.testclasses;

public enum TestEnum {

	NORMAL,

	SPECIAL {
		@Override
		public boolean isSpecial() {
			return true;
		}
	};

	public boolean isSpecial() {
		return false;
	}
}
