package de.cronn.reflection.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RecordUtilsTest {

	@Test
	void testCloneRecord() {
		record TestRecord(int a, int b) {
		}

		TestRecord record = new TestRecord(1, 2);

		TestRecord clone = RecordUtils.cloneRecord(record, value -> ((int) value) + 1);

		assertThat(clone.a()).isEqualTo(2);
		assertThat(clone.b()).isEqualTo(3);
	}

	@Test
	void testCloneRecord_notARecord() {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> RecordUtils.cloneRecord("not a record", value -> value))
			.withMessage("class java.lang.String is not a record");
	}
}
