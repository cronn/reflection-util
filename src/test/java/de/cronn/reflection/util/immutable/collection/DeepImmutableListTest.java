package de.cronn.reflection.util.immutable.collection;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import org.junit.jupiter.api.Test;

import de.cronn.reflection.util.immutable.ImmutableProxy;
import de.cronn.reflection.util.testclasses.OtherTestEntity;
import de.cronn.reflection.util.testclasses.TestEntity;

public class DeepImmutableListTest {

	@Test
	void testConstruction() throws Exception {
		assertThat(DeepImmutableList.of(1)).hasSize(1);
		assertThat(DeepImmutableList.of(1, 2)).hasSize(2);
		assertThat(DeepImmutableList.of(1, 2, 3)).hasSize(3);
	}

	@Test
	void testCreateImmutableProxy() throws Exception {
		List<TestEntity> immutableProxy = ImmutableProxy.create(Collections.singletonList(new TestEntity(123)));
		assertThat(ImmutableProxy.isImmutableProxy(immutableProxy));
		assertThat(immutableProxy.get(0).getNumber()).isEqualTo(123);
	}

	@Test
	void testWriteMethodsAreRejected() throws Exception {
		TestEntity testEntity = new TestEntity();
		testEntity.setSomeList(Arrays.asList(
			new OtherTestEntity("a"),
			new OtherTestEntity("b")
		));

		TestEntity immutableProxy = ImmutableProxy.create(testEntity);
		List<OtherTestEntity> immutableList = immutableProxy.getSomeList();

		assertThat(ImmutableProxy.isImmutableProxy(immutableList)).isTrue();

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.remove(null))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.addAll(Collections.emptyList()))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.retainAll(testEntity.getSomeList()))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.removeAll(testEntity.getSomeList()))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.sort(Comparator.comparing(OtherTestEntity::getName)))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.set(0, null))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.add(0, null))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.remove(0))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.addAll(0, Collections.emptyList()))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.subList(0, 1).clear())
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.listIterator().remove())
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.listIterator().set(null))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.listIterator().add(null))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableList.listIterator(1).remove())
			.withMessage("This list is immutable");
	}

	@Test
	void testReadOnlyMethods() throws Exception {
		TestEntity testEntity = new TestEntity();
		testEntity.setSomeList(Arrays.asList(
			new OtherTestEntity("a"),
			new OtherTestEntity("b")
		));

		TestEntity immutableProxy = ImmutableProxy.create(testEntity);
		List<OtherTestEntity> immutableList = immutableProxy.getSomeList();

		assertThat(immutableList.indexOf(testEntity.getSomeList().get(1))).isEqualTo(1);
		assertThat(immutableList.lastIndexOf(testEntity.getSomeList().get(1))).isEqualTo(1);

		assertThat(immutableList.subList(0, 1)).hasSize(1);
		assertThat(immutableList.subList(0, 1).get(0).getName()).isSameAs(immutableList.get(0).getName());

		assertThat(immutableList.subList(1, 2)).hasSize(1);
		assertThat(immutableList.subList(1, 2).get(0).getName()).isSameAs(immutableList.get(1).getName());

		ListIterator<OtherTestEntity> listIterator = immutableList.listIterator(0);
		assertThat(listIterator.hasNext()).isTrue();
		assertThat(listIterator.hasPrevious()).isFalse();
		assertThat(listIterator.next().getName()).isEqualTo(immutableList.get(0).getName());
		assertThat(listIterator.hasPrevious()).isTrue();
		assertThat(listIterator.next().getName()).isEqualTo(immutableList.get(1).getName());
		assertThat(listIterator.nextIndex()).isEqualTo(2);
		assertThat(listIterator.hasNext()).isFalse();
		assertThat(listIterator.hasPrevious()).isTrue();
		assertThat(listIterator.previousIndex()).isEqualTo(1);
		assertThat(listIterator.previous().getName()).isEqualTo(immutableList.get(0).getName());
		assertThat(listIterator.hasNext()).isTrue();

		assertThat(immutableList.listIterator(1).next().getName()).isEqualTo(immutableList.get(1).getName());
		assertThat(immutableList.listIterator(2).hasNext()).isFalse();
	}

}
