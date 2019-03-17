package de.cronn.reflection.util.immutable.collection;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import de.cronn.reflection.util.immutable.ImmutableProxy;
import de.cronn.reflection.util.testclasses.TestEntity;

public class DeepImmutableSetTest {

	@Test
	public void testConstruction() throws Exception {
		assertThat(DeepImmutableSet.of(1)).hasSize(1);
		assertThat(DeepImmutableSet.of(1, 2)).hasSize(2);
		assertThat(DeepImmutableSet.of(1, 2, 3)).hasSize(3);
		assertThat(DeepImmutableSet.of(2, 2, 2)).hasSize(1);
	}

	@Test
	public void testCreateImmutableProxy() throws Exception {
		Set<TestEntity> immutableProxy = ImmutableProxy.create(Collections.singleton(new TestEntity(123)));
		assertThat(ImmutableProxy.isImmutableProxy(immutableProxy));
		assertThat(immutableProxy.iterator().next().getNumber()).isEqualTo(123);
	}

}
