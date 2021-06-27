package de.cronn.reflection.util.immutable.collection;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import de.cronn.reflection.util.testclasses.FinalClass;

import org.junit.jupiter.api.Test;

import de.cronn.reflection.util.immutable.ImmutableProxy;
import de.cronn.reflection.util.testclasses.OtherTestEntity;
import de.cronn.reflection.util.testclasses.TestEntity;

public class DeepImmutableCollectionTest {

	@Test
	void testWriteMethodsAreRejected() throws Exception {
		TestEntity testEntity = new TestEntity();
		testEntity.setSomeList(Arrays.asList(
			new OtherTestEntity("a"),
			new OtherTestEntity("b")
		));

		TestEntity immutableProxy = ImmutableProxy.create(testEntity);
		Collection<OtherTestEntity> immutableCollection = immutableProxy.getSomeCollection();

		assertThat(ImmutableProxy.isImmutableProxy(immutableCollection)).isTrue();

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableCollection.remove(null))
			.withMessage("This collection is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableCollection.addAll(Collections.emptyList()))
			.withMessage("This collection is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableCollection.retainAll(testEntity.getSomeList()))
			.withMessage("This collection is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableCollection.removeAll(testEntity.getSomeList()))
			.withMessage("This collection is immutable");
	}

	@Test
	void testReadOnlyMethods() throws Exception {
		TestEntity testEntity = new TestEntity();
		testEntity.setSomeList(Arrays.asList(
			new OtherTestEntity("a"),
			new OtherTestEntity("b")
		));

		TestEntity immutableProxy = ImmutableProxy.create(testEntity);
		Collection<OtherTestEntity> immutableCollection = immutableProxy.getSomeCollection();

		assertThat(immutableCollection.containsAll(testEntity.getSomeList())).isTrue();
		assertThat(immutableCollection.contains(testEntity.getSomeList().get(0))).isTrue();
	}

	@Test
	void testCreateImmutableProxy() throws Exception {
		Collection<TestEntity> original = Collections.singleton(new TestEntity(123));
		Collection<TestEntity> immutableProxy = ImmutableProxy.create(original);
		assertThat(ImmutableProxy.isImmutableProxy(immutableProxy)).isTrue();
		assertThat(immutableProxy.iterator().next().getNumber()).isEqualTo(123);
	}

	@Test
	void shouldNotBeSoftByDefault() throws Exception {
		FinalClass finalClass = new FinalClass("");
		DeepImmutableCollection<FinalClass> immutableMap = new DeepImmutableCollection<>(singleton(finalClass));

		assertThrows(IllegalArgumentException.class, () -> immutableMap.iterator().next());
	}

}
