package de.cronn.reflection.util.immutable;

import static de.cronn.reflection.util.immutable.SoftImmutableProxy.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.cronn.reflection.util.testclasses.SomeClass;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import de.cronn.reflection.util.immutable.collection.DeepImmutableList;
import de.cronn.reflection.util.immutable.collection.DeepImmutableMap;
import de.cronn.reflection.util.immutable.collection.DeepImmutableSet;
import de.cronn.reflection.util.testclasses.ClassWithDefaultMethods;
import de.cronn.reflection.util.testclasses.ClassWithInheritedDefaultMethods;
import de.cronn.reflection.util.testclasses.FinalClass;
import de.cronn.reflection.util.testclasses.OtherTestEntity;
import de.cronn.reflection.util.testclasses.SubclassOfClassWithDefaultMethods;
import de.cronn.reflection.util.testclasses.TestEntity;
import de.cronn.reflection.util.testclasses.TestEnum;

public class ImmutableProxyTest {

	public static final String IMMUTABLE_EXCEPTION_MESSAGE = "This instance is immutable." +
															 " Annotate the method with @ReadOnly if this is a false-positive.";

	private static final long TEST_TIMEOUT_SECONDS = 30;

	@Test
	void testImmutableProxy() throws Exception {
		TestEntity original = new TestEntity(123);

		TestEntity immutableProxy = ImmutableProxy.create(original);

		assertThat(immutableProxy).hasSameHashCodeAs(original);
		assertThat(immutableProxy).hasToString(original.toString());

		assertThat(immutableProxy.getNumber()).isEqualTo(123);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.setNumber(456))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);
	}

	@Test
	void testUnwrap() throws Exception {
		TestEntity original = new TestEntity(123);

		TestEntity immutableProxy = ImmutableProxy.create(original);

		assertThat(ImmutableProxy.unwrap(immutableProxy)).isSameAs(original);
		assertThat(ImmutableProxy.unwrap(original)).isSameAs(original);
	}

	@Test
	void testImmutableProxyOnFinalClass() throws Exception {
		FinalClass finalClass = new FinalClass("");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> ImmutableProxy.create(finalClass))
			.withMessage("Cannot subclass primitive, array or final types: " + FinalClass.class);
	}

	@Test
	void testImmutableProxy_TestEntity() throws Exception {
		TestEntity original = new TestEntity(123);
		original.setSomeInstant(Instant.parse("2018-07-12T13:38:56Z"));
		original.setSomeUuid(UUID.fromString("28e93b24-7252-43d8-a223-ca0b3270bd7f"));
		original.setSomeFile(new File("some-file"));
		original.setSomePath(Paths.get("some path"));
		original.setSomeUri(new URI("file://some-path"));
		original.setSomeList(Arrays.asList(new OtherTestEntity("one"), new OtherTestEntity("other")));
		original.setSomeSet(new LinkedHashSet<>(Arrays.asList("a", "b", "c")));

		TestEntity immutableProxy = ImmutableProxy.create(original);

		assertThat(immutableProxy.getSomeList()).isNotSameAs(original.getSomeList());
		assertThat(immutableProxy.getSomeSet()).isNotSameAs(original.getSomeSet());

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.getSomeList().clear())
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.getSomeSet().clear())
			.withMessage("This set is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.getSomeCollection().clear())
			.withMessage("This collection is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.getSomeIterable().iterator().remove())
			.withMessage("This collection is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(immutableProxy::clear)
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);

		assertThat(immutableProxy.countSomeSet()).isEqualTo(immutableProxy.getSomeSet().size());
		assertThat(immutableProxy.countSomeList()).isEqualTo(immutableProxy.getSomeList().size());
		assertThat(immutableProxy.countNothing()).isZero();

		assertThat(immutableProxy.asMyself()).isInstanceOf(Immutable.class);
		assertThat(immutableProxy.asMyself().getSomeInstant()).isSameAs(original.getSomeInstant());
		assertThat(immutableProxy.asMyself().getSomeUuid()).isSameAs(original.getSomeUuid());
		assertThat(immutableProxy.asMyself().getSomeFile()).isSameAs(original.getSomeFile());
		assertThat(immutableProxy.asMyself().getSomePath()).isSameAs(original.getSomePath());
		assertThat(immutableProxy.asMyself().getSomeUri()).isSameAs(original.getSomeUri());
	}

	@Test
	void testImmutableProxy_fluentSetter() throws Exception {
		TestEntity original = new TestEntity(123);
		original.setSomeUuid(UUID.fromString("28e93b24-7252-43d8-a223-ca0b3270bd7f"));

		TestEntity immutableProxy = ImmutableProxy.create(original);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.someUuid(UUID.fromString("abcdef00-cafe-beef-abcd-987654321000")))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);
	}

	@Test
	void testDate() throws Exception {
		Date original = new Date(12345678);

		Date immutableDate = ImmutableProxy.create(original);

		assertThat(immutableDate).isInstanceOf(Immutable.class);
		assertThat(immutableDate).hasTime(12345678L);
		assertThat(immutableDate.clone()).isEqualTo(original);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableDate.setTime(12345L))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);
	}

	@Test
	void testImmutableProxyOnObject() throws Exception {
		Object original = new Object();

		Object immutableProxy = ImmutableProxy.create(original);

		assertThat(immutableProxy).hasSameHashCodeAs(original);
		assertThat(immutableProxy).isEqualTo(original);
	}

	@Test
	void testImmutableProxyIsAView() throws Exception {
		TestEntity original = new TestEntity(123);

		TestEntity immutableProxy = ImmutableProxy.create(original);

		original.setNumber(456);
		assertThat(immutableProxy.getNumber()).isEqualTo(456);
	}

	@Test
	void testImmutableProxy_ReferencedEntityIsAlsoImmutable() throws Exception {
		TestEntity original = new TestEntity();
		original.setOtherTestEntity(new OtherTestEntity());
		TestEntity immutableProxy = ImmutableProxy.create(original);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.getOtherTestEntity().setName("other name"))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);
	}

	@Test
	void testImmutableProxy_Collection() throws Exception {
		TestEntity original = new TestEntity();
		original.setSomeList(new ArrayList<>());
		original.setSomeSet(new LinkedHashSet<>());

		TestEntity immutableProxy = ImmutableProxy.create(original);

		List<OtherTestEntity> immutableList = immutableProxy.getSomeList();
		assertThat(immutableList).isEqualTo(original.getSomeList());
		assertThat(immutableList).hasSameSizeAs(original.getSomeList());
		assertThat(immutableList).hasSameHashCodeAs(original.getSomeList());

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.add(new OtherTestEntity()))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.getSomeSet().add("new value"))
			.withMessage("This set is immutable");

		assertThat(immutableList).isEmpty();

		original.getSomeList().add(new OtherTestEntity());
		assertThat(immutableList).hasSize(1);
		assertThat(immutableList).isNotEmpty();

		original.getSomeList().add(new OtherTestEntity());
		assertThat(immutableList).hasSize(2);

		assertThat(immutableList.get(0)).isSameAs(immutableList.get(0));

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.get(0).setName("new name"))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> ((OtherTestEntity) immutableList.toArray()[0]).setName("new name"))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.toArray(new OtherTestEntity[0])[0].setName("new name"))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.stream().findFirst().get().setName("new name"))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.forEach(entity -> entity.setName("new name")))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.removeIf(w -> true))
			.withMessage("This list is immutable");
	}

	@Test
	void testImmutableProxy_TooSpecificReturnType() throws Exception {
		TestEntity original = new TestEntity();
		original.setSomeList(Collections.emptyList());
		TestEntity immutableProxy = ImmutableProxy.create(original);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(immutableProxy::getSomeArrayList)
			.withMessage("Cannot create immutable collection for TestEntity.getSomeArrayList." +
						 " The return type is unknown or too specific: class java.util.ArrayList." +
						 " Consider to define a more generic type: Set/List/Collection");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(immutableProxy::getSomeTreeMap)
			.withMessage("Cannot create immutable map for TestEntity.getSomeTreeMap." +
						 " The return type is unknown or too specific: class java.util.TreeMap." +
						 " Consider to define a more generic type: Map");
	}

	@Test
	void testImmutableProxy_Collection_Stream() throws Exception {
		Collection<TestEntity> entities = Arrays.asList(new TestEntity(1), new TestEntity(2));

		Collection<TestEntity> immutableCollection = ImmutableProxy.create(entities);
		assertThat(immutableCollection.stream().anyMatch(wheel -> wheel.getNumber() == 1)).isTrue();

		List<TestEntity> immutableList = ImmutableProxy.create(new ArrayList<>(entities));
		assertThat(immutableList.stream().anyMatch(wheel -> wheel.getNumber() == 1)).isTrue();

		Set<TestEntity> immutableSet = ImmutableProxy.create(new LinkedHashSet<>(entities));
		assertThat(immutableSet.stream().anyMatch(wheel -> wheel.getNumber() == 1)).isTrue();
	}

	@Test
	void testImmutableProxy_Collection_Iterator() throws Exception {
		Collection<TestEntity> entities = Arrays.asList(new TestEntity(1), new TestEntity(2));

		Collection<TestEntity> immutableEntityCollection = ImmutableProxy.create(entities);
		List<TestEntity> immutableEntityList = ImmutableProxy.create(new ArrayList<>(entities));
		Set<TestEntity> immutableEntitySet = ImmutableProxy.create(new LinkedHashSet<>(entities));

		// iterator next -> readOnly should work

		assertThat(immutableEntityCollection.iterator().next().getNumber()).isEqualTo(1);
		assertThat(immutableEntityList.iterator().next().getNumber()).isEqualTo(1);
		assertThat(immutableEntitySet.iterator().next().getNumber()).isEqualTo(1);

		// iterator next -> modification should be forbidden

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableEntityCollection.iterator().next().setNumber(123))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableEntityList.iterator().next().setNumber(123))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableEntitySet.iterator().next().setNumber(123))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);

		// iterator remove should be forbidden

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableEntityCollection.iterator().remove())
			.withMessage("This collection is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableEntityList.iterator().remove())
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableEntitySet.iterator().remove())
			.withMessage("This set is immutable");
	}

	@Test
	void testImmutableProxy_Map() throws Exception {
		TestEntity original = new TestEntity();
		original.setSomeMap(new LinkedHashMap<>());
		original.getSomeMap().put("a", new OtherTestEntity("a"));
		original.getSomeMap().put("b", new OtherTestEntity("b"));

		TestEntity immutableProxy = ImmutableProxy.create(original);

		Map<String, OtherTestEntity> immutableMap = immutableProxy.getSomeMap();
		assertThat(ImmutableProxySupport.isImmutable(immutableMap, SOFT_IMMUTABLE_DEFAULT)).isTrue();
		assertThat(immutableMap).hasSameSizeAs(original.getSomeMap());
		assertThat(immutableMap.get("a").getImmutableValue()).isEqualTo("a");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableMap.get("a").setName("new name"))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);
	}

	// https://github.com/cronn/reflection-util/issues/5
	@Test
	void testImmutableProxy_MapWithList() throws Exception {
		List<Integer> origin = new ArrayList<>();
		origin.add(1);
		origin.add(2);

		Map<String, List<Integer>> wrapper = new HashMap<>();

		wrapper.put("AAA", origin);

		Map<String, List<Integer>> immutable = ImmutableProxy.create(wrapper);
		assertThat(immutable).isInstanceOf(DeepImmutableMap.class);

		List<Integer> list = immutable.get("AAA");
		assertThat(list).isInstanceOf(DeepImmutableList.class);
		assertThat(list).containsExactly(1, 2);
		assertThat(list.get(0)).isEqualTo(1);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> list.set(0, 3))
			.withMessage("This list is immutable");
	}

	@Test
	void testImmutableProxy_ListWithList() throws Exception {
		List<Integer> origin = new ArrayList<>();
		origin.add(1);
		origin.add(2);

		List<List<Integer>> wrapper = new ArrayList<>();

		wrapper.add(origin);

		List<List<Integer>> immutable = ImmutableProxy.create(wrapper);
		assertThat(immutable).isInstanceOf(DeepImmutableList.class);

		List<Integer> list = immutable.get(0);
		assertThat(list).isInstanceOf(DeepImmutableList.class);
		assertThat(list).containsExactly(1, 2);
		assertThat(list.get(0)).isEqualTo(1);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> list.set(0, 3))
			.withMessage("This list is immutable");
	}

	@Test
	void testImmutableProxy_MapWithSet() throws Exception {
		Set<Integer> origin = new LinkedHashSet<>();
		origin.add(1);
		origin.add(2);

		Map<String, Set<Integer>> wrapper = new HashMap<>();

		wrapper.put("AAA", origin);

		Map<String, Set<Integer>> immutable = ImmutableProxy.create(wrapper);
		assertThat(immutable).isInstanceOf(DeepImmutableMap.class);

		Set<Integer> set = immutable.get("AAA");
		assertThat(set).isInstanceOf(DeepImmutableSet.class);
		assertThat(set).containsExactly(1, 2);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> set.add(3))
			.withMessage("This set is immutable");
	}

	@Test
	void testImmutableProxy_MapWithMap() throws Exception {
		Map<String, Integer> origin = Collections.singletonMap("a", 123);

		Map<String, Map<String, Integer>> wrapper = new HashMap<>();

		wrapper.put("AAA", origin);

		Map<String, Map<String, Integer>> immutable = ImmutableProxy.create(wrapper);
		assertThat(immutable).isInstanceOf(DeepImmutableMap.class);

		Map<String, Integer> map = immutable.get("AAA");
		assertThat(map).isInstanceOf(DeepImmutableMap.class);
		assertThat(map).containsExactly(entry("a", 123));

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> map.put("b", 345))
			.withMessage("This map is immutable");
	}

	@Test
	void testImmutableProxyOnImmutableValue() throws Exception {
		Object nullValue = null;

		assertThat(ImmutableProxy.create(nullValue)).isNull();
		assertImmutableProxyReturnsSameInstance("abc");
		assertImmutableProxyReturnsSameInstance(5L);
		assertImmutableProxyReturnsSameInstance(10);
		assertImmutableProxyReturnsSameInstance(1.5);
		assertImmutableProxyReturnsSameInstance(3.14F);
		assertImmutableProxyReturnsSameInstance(true);
		assertImmutableProxyReturnsSameInstance('a');
		assertImmutableProxyReturnsSameInstance(LocalDate.of(2018, Month.JULY, 12));
		assertImmutableProxyReturnsSameInstance(Instant.parse("2019-03-17T11:19:38.000Z"));
		assertImmutableProxyReturnsSameInstance(ZonedDateTime.parse("2019-03-17T11:19:38.000+02:00"));
		assertImmutableProxyReturnsSameInstance(Duration.ofSeconds(5));
		assertImmutableProxyReturnsSameInstance(TestEnum.NORMAL);
		assertImmutableProxyReturnsSameInstance(TestEnum.SPECIAL);
		assertImmutableProxyReturnsSameInstance(TestEnum.SPECIAL);
		assertImmutableProxyReturnsSameInstance(DeepImmutableSet.of("foo"));
		assertImmutableProxyReturnsSameInstance(DeepImmutableList.of("bar"));
	}

	private static void assertImmutableProxyReturnsSameInstance(Object value) {
		assertThat(ImmutableProxy.create(value)).isSameAs(value);
	}

	@Test
	void testImmutableProxyIsEqualToOriginal() throws Exception {
		TestEntity original = new TestEntity(123);

		TestEntity immutableProxy = ImmutableProxy.create(original);

		assertThat(immutableProxy).isEqualTo(original);
	}

	@Test
	void testImmutableProxyOnImmutableProxy() throws Exception {
		TestEntity original = new TestEntity(123);

		TestEntity proxy1 = ImmutableProxy.create(original);
		TestEntity proxy2 = ImmutableProxy.create(proxy1);

		assertThat(proxy2).isSameAs(proxy1);
	}

	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	void testConcurrentlyCreateProxy() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(5);
		try {
			CompletionService<TestEntity> completionService = new ExecutorCompletionService<>(executorService);
			for (int x = 0; x < 50; x++) {
				TestEntity entity = new TestEntity(100 + x);
				int numRepetitions = 20;
				for (int i = 0; i < numRepetitions; i++) {
					completionService.submit(() -> ImmutableProxy.create(entity));
				}
				for (int i = 0; i < numRepetitions; i++) {
					TestEntity immutableProxy = completionService.take().get();
					assertThat(immutableProxy).isNotSameAs(entity);
					assertThat(immutableProxy.getNumber()).isEqualTo(entity.getNumber());
				}
				ImmutableProxy.clearCache();
			}
		} finally {
			executorService.shutdown();
			executorService.awaitTermination(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}
	}

	@Test
	void testIsImmutableProxy() throws Exception {
		assertThat(ImmutableProxy.isImmutableProxy(null)).isFalse();
		assertThat(ImmutableProxy.isImmutableProxy(ImmutableProxy.create("foo"))).isFalse();
		assertThat(ImmutableProxy.isImmutableProxy(ImmutableProxy.create(new TestEntity()))).isTrue();
	}

	@Test
	void testReadOnlyAnnotationsInInterface() throws Exception {
		assertThat(ImmutableProxy.create(new ClassWithInheritedDefaultMethods()).size()).isEqualTo(0);
		assertThat(ImmutableProxy.create(new SubclassOfClassWithDefaultMethods()).size()).isEqualTo(0);
		assertThat(ImmutableProxy.create(new ClassWithDefaultMethods()).size()).isEqualTo(0);
	}

	@Test
	void testDoNotProxyReturnValueIfDisabledByReadOnlyAnnotation() throws Exception {
		TestEntity proxy = ImmutableProxy.create(new TestEntity(123));

		TestEntity reference = proxy.asReference();
		assertThat(reference.getNumber()).isEqualTo(123);

		assertThat(ImmutableProxy.isImmutableProxy(reference)).isFalse();
		reference.setNumber(456);
		assertThat(reference.getNumber()).isEqualTo(456);

		TestEntity immutableReference = (TestEntity) proxy.asReferenceImmutableProxy();
		assertThat(ImmutableProxy.isImmutableProxy(immutableReference)).isTrue();

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableReference.setNumber(456))
			.withMessage(IMMUTABLE_EXCEPTION_MESSAGE);
	}

	@Test
	void testDoNotProxyReturnValueInCloneMethod() throws Exception {
		TestEntity proxy = ImmutableProxy.create(new TestEntity(123));

		TestEntity clone = proxy.clone();
		assertThat(clone.getNumber()).isEqualTo(123);

		assertThat(ImmutableProxy.isImmutableProxy(clone)).isFalse();
		clone.setNumber(456);
		assertThat(clone.getNumber()).isEqualTo(456);
	}

	@Test
	void testSerializeDeepImmutableSet() throws Exception {
		DeepImmutableSet<String> proxy = (DeepImmutableSet<String>) ImmutableProxy.create(Collections.singleton("foo"));
		DeepImmutableSet<String> clone = SerializationUtils.clone(proxy);
		assertThat(clone).isNotSameAs(proxy).containsExactly("foo");
	}

	@Test
	void testSerializeDeepImmutableList() throws Exception {
		DeepImmutableList<String> proxy = (DeepImmutableList<String>) ImmutableProxy.create(Arrays.asList("a", "b", "c"));
		DeepImmutableList<String> clone = SerializationUtils.clone(proxy);
		assertThat(clone).isNotSameAs(proxy).containsExactly("a", "b", "c");
	}

	@Test
	void testSerializeDeepImmutableMap() throws Exception {
		DeepImmutableMap<String, String> proxy = (DeepImmutableMap<String, String>) ImmutableProxy.create(Collections.singletonMap("k", "v"));
		DeepImmutableMap<String, String> clone = SerializationUtils.clone(proxy);
		assertThat(clone).isNotSameAs(proxy).containsExactly(entry("k", "v"));
	}

	@Test
	void testSerializeImmutableProxy() throws Exception {
		TestEntity original = new TestEntity(123);
		original.setSomeList(Arrays.asList(
			new OtherTestEntity("one"),
			new OtherTestEntity("other")
		));

		TestEntity proxy = ImmutableProxy.create(original);

		OtherTestEntity firstElementBefore = proxy.getSomeList().get(0);

		TestEntity clone = SerializationUtils.clone(proxy);
		assertThat(clone).isInstanceOf(Immutable.class);

		OtherTestEntity firstElementAfter = clone.getSomeList().get(0);
		assertThat(firstElementAfter).isNotSameAs(firstElementBefore);
	}

	@Test
	void testIsImmutableProxyClass() throws Exception {
		SomeClass target = new SomeClass();
		SomeClass proxy = ImmutableProxy.create(target);

		assertAll(
			() -> assertThat(ImmutableProxy.isImmutableProxyClass(target.getClass())).isFalse(),
			() -> assertThat(ImmutableProxy.isImmutableProxyClass(proxy.getClass())).isTrue()
		);
	}

}
