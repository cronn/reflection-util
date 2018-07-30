package de.cronn.reflection.util.testclasses;

import java.util.Arrays;

import javax.annotation.Nullable;
import javax.validation.constraints.Size;

public class TestEntity {

	@Size(min = 10, max = 20)
	private int number;

	@Size(min = 0, max = 1000)
	private String string;

	@Nullable
	private Object someObject = Arrays.asList("abc", "def");

	private Object fieldWithoutGetter;

	private String fieldWithAnnotationOnSetter;

	public void setNumber(int number) {
		this.number = number;
	}

	public int getNumber() {
		return number;
	}

	public void setString(String string) {
		this.string = string;
	}

	public String getString() {
		return string;
	}

	public void setSomeObject(Object someObject) {
		this.someObject = someObject;
	}

	public Object getSomeObject() {
		return someObject;
	}

	public int[] getPropertyWithoutField() {
		return null;
	}

	public Object doNothing() {
		return null;
	}

	public void setFieldWithoutGetter(Object fieldWithoutGetter) {
		this.fieldWithoutGetter = fieldWithoutGetter;
	}

	public Object getPropertyWithExceptionInGetter() {
		throw new UnsupportedOperationException();
	}

	public String getFieldWithAnnotationOnSetter() {
		return fieldWithAnnotationOnSetter;
	}

	@Size(min = 10, max = 20)
	public void setFieldWithAnnotationOnSetter(String fieldWithAnnotationOnSetter) {
		this.fieldWithAnnotationOnSetter = fieldWithAnnotationOnSetter;
	}
}
