package de.cronn.reflection.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import de.cronn.reflection.util.RecordSupport.ArrayUtils;

class RecordSupportTest {

	@Test
	void testArrayUtils_indexOf() throws Exception {
		assertThat(ArrayUtils.indexOf(new Integer[0], 1)).isEqualTo(-1);
		assertThat(ArrayUtils.indexOf(new Integer[] { 3, 2, 1 }, 1)).isEqualTo(2);
		assertThat(ArrayUtils.indexOf(new Integer[] { 3, 2, 2, 1, 1 }, 1)).isEqualTo(3);
		assertThat(ArrayUtils.indexOf(new Integer[] { 3, 2, 2, 1, 1 }, 4)).isEqualTo(-1);
		assertThat(ArrayUtils.indexOf(new Integer[] { 1, 2, 3 }, "x")).isEqualTo(-1);
	}

	@Test
	void testArrayUtils_lastIndexOf() throws Exception {
		assertThat(ArrayUtils.lastIndexOf(new Integer[0], 1)).isEqualTo(-1);
		assertThat(ArrayUtils.lastIndexOf(new Integer[] { 3, 2, 1 }, 1)).isEqualTo(2);
		assertThat(ArrayUtils.lastIndexOf(new Integer[] { 3, 2, 2, 1, 1 }, 1)).isEqualTo(4);
		assertThat(ArrayUtils.lastIndexOf(new Integer[] { 3, 2, 2, 1, 1 }, 4)).isEqualTo(-1);
		assertThat(ArrayUtils.lastIndexOf(new Integer[] { 1, 2, 3 }, "x")).isEqualTo(-1);
	}

}
