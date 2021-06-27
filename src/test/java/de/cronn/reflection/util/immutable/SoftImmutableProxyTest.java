package de.cronn.reflection.util.immutable;

import static de.cronn.reflection.util.immutable.SoftImmutableProxy.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.cronn.reflection.util.testclasses.FinalClass;
import de.cronn.reflection.util.testclasses.FinalClassContainer;
import de.cronn.reflection.util.testclasses.SomeClass;

public class SoftImmutableProxyTest {

	@BeforeEach
	void reset() {
		clearCache();
	}

	@Test
	void shouldCreateProxy() throws Exception {
		SomeClass someClass = new SomeClass();

		SomeClass proxy = create(someClass);

		assertAll(
			() -> assertThat(proxy).isNotSameAs(someClass),
			() -> assertThat(isImmutableProxy(proxy)).isTrue()
		);
	}

	@Test
	void shouldNotCreateProxyForFinalClass() throws Exception {
		FinalClass finalClass = new FinalClass("");

		FinalClass proxy = create(finalClass);

		assertAll(
			() -> assertThat(proxy).isSameAs(finalClass),
			() -> assertThat(isImmutableProxy(proxy)).isFalse()
		);
	}

	@Test
	void shouldNotFailOnFinalClasses_list() throws Exception {
		FinalClass finalClass = new FinalClass("");
		List<FinalClass> list = new ArrayList<>();
		list.add(finalClass);

		List<FinalClass> listProxy = create(list);

		FinalClass finalClass1 = listProxy.get(0);
		assertAll(
			() -> assertThat(finalClass1).isSameAs(finalClass),
			() -> assertThat(isImmutableProxy(finalClass1)).isFalse()
		);
	}

	@Test
	void shouldNotFailOnFinalClasses_set() throws Exception {
		FinalClass finalClass = new FinalClass("");
		Set<FinalClass> set = new HashSet<>();
		set.add(finalClass);

		Set<FinalClass> setProxy = create(set);

		FinalClass finalClass1 = setProxy.iterator().next();
		assertAll(
			() -> assertThat(finalClass1).isSameAs(finalClass),
			() -> assertThat(isImmutableProxy(finalClass1)).isFalse()
		);
	}

	@Test
	void shouldNotFailOnFinalClasses_map() throws Exception {
		FinalClass finalClass = new FinalClass("");
		Map<String, FinalClass> map = new HashMap<>();
		map.put("key", finalClass);

		Map<String, FinalClass> mapProxy = create(map);

		FinalClass finalClass1 = mapProxy.get("key");
		assertAll(
			() -> assertThat(finalClass1).isSameAs(finalClass),
			() -> assertThat(isImmutableProxy(finalClass1)).isFalse()
		);
	}

	@Test
	void shouldNotFailOnFinalClasses_nested() throws Exception {
		FinalClass finalClass = new FinalClass("");
		FinalClassContainer finalClassContainer = new FinalClassContainer(finalClass);

		FinalClassContainer proxy = create(finalClassContainer);

		FinalClass finalClass1 = proxy.getFinalClass();
		assertAll(
			() -> assertThat(finalClass1).isSameAs(finalClass),
			() -> assertThat(isImmutableProxy(finalClass1)).isFalse(),
			() -> assertThat(finalClass1.getSomeProperty()).isEqualTo("")
		);
	}

	@Test
	void shouldUnwrapObjects() {
		SomeClass target = new SomeClass();
		SomeClass proxy = create(target);
		SomeClass unwrapped = unwrap(proxy);

		assertAll(
			() -> assertThat(isImmutableProxy(proxy)).isTrue(),
			() -> assertThat(isImmutableProxy(unwrapped)).isFalse(),
			() -> assertThat(unwrapped).isSameAs(target)
		);
	}

	@Test
	void shouldUnwrapCollections() {
		SomeClass target = new SomeClass();
		Collection<SomeClass> collection = Collections.singleton(target);
		Collection<SomeClass> proxy = create(collection);
		Collection<SomeClass> unwrapped = unwrap(proxy);

		assertAll(
			() -> assertThat(isImmutableProxy(proxy)).isTrue(),
			() -> assertThat(isImmutableProxy(unwrapped)).isFalse(),
			() -> assertThat(unwrapped).isSameAs(collection),
			() -> assertThat(isImmutableProxy(unwrapped.iterator().next())).isFalse(),
			() -> assertThat(unwrapped.iterator().next()).isSameAs(target)
		);
	}

	@Test
	void shouldCreateProxyCollection() {
		SomeClass target = new SomeClass();
		Collection<SomeClass> collection = Collections.singleton(target);
		Collection<SomeClass> proxy = create(collection);

		assertAll(
			() -> assertThat(isImmutableProxy(proxy)).isTrue(),
			() -> assertThat(isImmutableProxy(proxy.iterator().next())).isTrue(),
			() -> assertThat(proxy.iterator().next()).isEqualTo(target)
		);
	}
}
