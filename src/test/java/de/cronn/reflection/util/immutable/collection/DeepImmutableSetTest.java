package de.cronn.reflection.util.immutable.collection;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class DeepImmutableSetTest {

	@Test
	public void testConstruction() throws Exception {
		assertThat(DeepImmutableSet.of(1)).hasSize(1);
		assertThat(DeepImmutableSet.of(1, 2)).hasSize(2);
		assertThat(DeepImmutableSet.of(1, 2, 3)).hasSize(3);
		assertThat(DeepImmutableSet.of(2, 2, 2)).hasSize(1);
	}

}
