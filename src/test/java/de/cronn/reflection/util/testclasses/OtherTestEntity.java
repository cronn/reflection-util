package de.cronn.reflection.util.testclasses;

import java.util.ArrayList;
import java.util.List;

public class OtherTestEntity {

	private String name;
	private final String immutableValue;
	private final List<String> children = new ArrayList<>();

	public OtherTestEntity() {
		this(null);
	}

	public OtherTestEntity(String immutableValue) {
		this.immutableValue = immutableValue;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getImmutableValue() {
		return immutableValue;
	}

	public List<String> getChildren() {
		return children;
	}

}
