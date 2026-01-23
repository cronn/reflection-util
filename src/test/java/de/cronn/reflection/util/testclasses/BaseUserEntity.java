package de.cronn.reflection.util.testclasses;

public class BaseUserEntity implements UserDetails {
	private boolean enabled;

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
