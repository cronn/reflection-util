package de.cronn.reflection.util.testclasses;

public class ExtendedUserEntity extends BaseUserEntity {
	@Override
	public boolean isEnabled() {
		// Override the base class implementation
		return super.isEnabled();
	}
}
