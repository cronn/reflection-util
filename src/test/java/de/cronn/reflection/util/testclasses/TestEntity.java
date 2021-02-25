package de.cronn.reflection.util.testclasses;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class TestEntity extends AbstractClassWithAnnotatedMethods implements InterfaceWithReadOnlyMethods, Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	@Size(min = 10, max = 20)
	private int number;

	@Size(min = 0, max = 1000)
	private String string;

	@NotNull
	private Object someObject = Arrays.asList("abc", "def");

	private Object fieldWithoutGetter;

	private String fieldWithAnnotationOnSetter;

	private Instant someInstant;

	private UUID someUuid;

	private File someFile;

	private Path somePath;

	private URI someUri;

	private OtherTestEntity otherTestEntity;

	private List<OtherTestEntity> someList;

	private Set<String> someSet;

	private Map<String, OtherTestEntity> someMap;

	public TestEntity() {
	}

	public TestEntity(int number) {
		this.number = number;
	}

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

	public Instant getSomeInstant() {
		return someInstant;
	}

	public void setSomeInstant(Instant someInstant) {
		this.someInstant = someInstant;
	}

	public UUID getSomeUuid() {
		return someUuid;
	}

	public void setSomeUuid(UUID someUuid) {
		this.someUuid = someUuid;
	}

	public File getSomeFile() {
		return someFile;
	}

	public void setSomeFile(File someFile) {
		this.someFile = someFile;
	}

	public Path getSomePath() {
		return somePath;
	}

	public void setSomePath(Path somePath) {
		this.somePath = somePath;
	}

	public URI getSomeUri() {
		return someUri;
	}

	public void setSomeUri(URI someUri) {
		this.someUri = someUri;
	}

	public OtherTestEntity getOtherTestEntity() {
		return otherTestEntity;
	}

	public void setOtherTestEntity(OtherTestEntity otherTestEntity) {
		this.otherTestEntity = otherTestEntity;
	}

	public List<OtherTestEntity> getSomeList() {
		return someList;
	}

	public void setSomeList(List<OtherTestEntity> someList) {
		this.someList = someList;
	}

	public void clear() {
		getSomeList().clear();
		getSomeSet().clear();
		getSomeCollection().clear();
	}

	public Set<String> getSomeSet() {
		return someSet;
	}

	public Collection<OtherTestEntity> getSomeCollection() {
		return getSomeList();
	}

	public Iterable<OtherTestEntity> getSomeIterable() {
		return getSomeList();
	}

	public void setSomeSet(Set<String> someSet) {
		this.someSet = someSet;
	}

	public ArrayList<OtherTestEntity> getSomeArrayList() {
		return new ArrayList<>(getSomeList());
	}

	public Map<String, OtherTestEntity> getSomeMap() {
		return someMap;
	}

	public void setSomeMap(Map<String, OtherTestEntity> someMap) {
		this.someMap = someMap;
	}

	@Override
	public int countSomeList() {
		return getSomeList().size();
	}

	@Override
	public int countSomeSet() {
		return getSomeSet().size();
	}

	@Override
	public TestEntity asMyself() {
		return this;
	}

	@Override
	public TestEntity asReference() {
		return new TestEntity(getNumber());
	}

	@Override
	public TestEntity clone() throws CloneNotSupportedException {
		return (TestEntity) super.clone();
	}

	public TreeMap<String, String> getSomeTreeMap() {
		return new TreeMap<>();
	}
}
