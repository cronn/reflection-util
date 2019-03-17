package de.cronn.reflection.util.immutable.collection;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import de.cronn.reflection.util.immutable.ImmutableProxy;
import de.cronn.reflection.util.immutable.ImmutableProxyTest;
import de.cronn.reflection.util.testclasses.OtherTestEntity;
import de.cronn.reflection.util.testclasses.TestEntity;

public class DeepImmutableMapTest {

	@Test
	public void testImmutableMap() throws Exception {
		TestEntity original = new TestEntity();
		original.setSomeMap(new LinkedHashMap<>());
		original.getSomeMap().put("a", new OtherTestEntity("a"));
		original.getSomeMap().put("b", new OtherTestEntity("b"));

		TestEntity immutableProxy = ImmutableProxy.create(original);

		Map<String, OtherTestEntity> immutableMap = immutableProxy.getSomeMap();

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableMap.remove("a"))
			.withMessage("This map is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(immutableMap::clear)
			.withMessage("This map is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableMap.computeIfAbsent("key", OtherTestEntity::new))
			.withMessage("This map is immutable");

		assertThat(immutableMap.keySet()).containsExactly("a", "b");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableMap.entrySet().clear())
			.withMessage("This map is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableMap.entrySet().retainAll(Collections.emptyList()))
			.withMessage("This map is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableMap.entrySet().iterator().remove())
			.withMessage("This map is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableMap.entrySet().iterator().next().setValue(null))
			.withMessage("This map is immutable");

		OtherTestEntity valueFromEntrySetIterator = immutableMap.entrySet().iterator().next().getValue();
		assertThat(valueFromEntrySetIterator).isSameAs(immutableMap.get("a"));

		OtherTestEntity valueFromValueIterator = immutableMap.values().iterator().next();
		assertThat(valueFromValueIterator).isSameAs(immutableMap.get("a"));

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableMap.values().iterator().remove())
			.withMessage("This map is immutable");
	}

	@Test
	public void testImmutableMap_ImmutableKey() throws Exception {
		Map<Date, Object> immutableMap = ImmutableProxy.create(Collections.singletonMap(new Date(123L), null));
		Date immutableValue = immutableMap.keySet().iterator().next();

		assertThat(immutableValue.getTime()).isEqualTo(123L);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableValue.setTime(456L))
			.withMessage(ImmutableProxyTest.IMMUTABLE_EXCEPTION_MESSAGE);
	}

}
