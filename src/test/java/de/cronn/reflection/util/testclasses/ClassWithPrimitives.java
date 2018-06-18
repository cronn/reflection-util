package de.cronn.reflection.util.testclasses;

public class ClassWithPrimitives {

	private boolean active;
	private long largeNumber;
	private float floatingNumber;
	private double doubleNumber;

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public void setLargeNumber(long largeNumber) {
		this.largeNumber = largeNumber;
	}

	public long getLargeNumber() {
		return largeNumber;
	}

	public float getFloatingNumber() {
		return floatingNumber;
	}

	public double getDoubleNumber() {
		return doubleNumber;
	}
}
