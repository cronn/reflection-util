package de.cronn.reflection.util;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import de.cronn.reflection.util.immutable.ImmutableProxy;
import de.cronn.reflection.util.immutable.ImmutableProxyOption;
import de.cronn.reflection.util.testclasses.Point;
import de.cronn.reflection.util.testclasses.RecordWithAtomicLong;
import de.cronn.reflection.util.testclasses.RecordWithList;
import de.cronn.reflection.util.testclasses.TestBeanWithListOfRecordHavingList;
import de.cronn.reflection.util.testclasses.TestBeanWithRecordField;
import de.cronn.reflection.util.testclasses.TestBeanWithRecordHavingAtomicLong;
import de.cronn.reflection.util.testclasses.TestBeanWithRecordHavingList;

class ImmutableProxyRecordTest {

	@Test
	void testCreateImmutableProxyOfRecordWithPrimitives() {
		record TestRecord(int a, int b, String string, boolean flag) {
		}

		TestRecord record = new TestRecord(1, 2, "data", true);
		TestRecord proxy = ImmutableProxy.create(record);
		assertThat(proxy).isSameAs(record);
	}

	@Test
	void testCreateImmutableProxyOfClassWithRecord() {
		TestBeanWithRecordField bean = new TestBeanWithRecordField();
		bean.setData("some value");

		TestBeanWithRecordField immutableProxy = ImmutableProxy.create(bean);

		Assertions.assertThat(immutableProxy.getData()).isEqualTo("some value");

		Assertions.assertThat(immutableProxy.getPoint()).isNull();

		bean.setPoint(new Point(1, 2));
		Assertions.assertThat(immutableProxy.getPoint()).isSameAs(bean.getPoint());
		Assertions.assertThat(immutableProxy.getPoint().x()).isEqualTo(1);
		Assertions.assertThat(immutableProxy.getPoint().y()).isEqualTo(2);
	}

	@Test
	void testCreateImmutableProxyOfClassWithRecordHavingList() {
		TestBeanWithRecordHavingList bean = new TestBeanWithRecordHavingList();
		bean.setRecordWithList(new RecordWithList(new ArrayList<>()));

		TestBeanWithRecordHavingList immutableProxy = ImmutableProxy.create(bean);

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(immutableProxy::getRecordWithList)
			.withMessage("class de.cronn.reflection.util.testclasses.RecordWithList is a record" +
						 " that potentially contains mutable components." +
						 " Consider using ImmutableProxy.create(bean, ImmutableProxyOption.ALLOW_CLONING_RECORDS)" +
						 " to enable cloning of such records.");
	}

	@Test
	void testCreateImmutableProxyOfClassWithRecordHavingList_allowCloningOfRecords() {
		TestBeanWithRecordHavingList bean = new TestBeanWithRecordHavingList();
		bean.setData("some value");
		bean.setRecordWithList(new RecordWithList(new ArrayList<>()));

		TestBeanWithRecordHavingList immutableProxy = ImmutableProxy.create(bean, ImmutableProxyOption.ALLOW_CLONING_RECORDS);

		bean.getRecordWithList().values().add("one");

		Assertions.assertThat(immutableProxy.getData()).isEqualTo("some value");

		Assertions.assertThat(immutableProxy.getRecordWithList().values()).containsExactly("one");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.getRecordWithList().values().add("two"))
			.withMessage("This list is immutable");
	}

	@Test
	void testCreateImmutableProxyOfClassWithRecordHavingList_allowCloningOfRecords_listOfRecords() {
		List<RecordWithList> recordWithLists = List.of(
			new RecordWithList(new ArrayList<>(List.of("one", "two", "three"))),
			new RecordWithList(new ArrayList<>(List.of("five", "six", "seven")))
		);

		List<RecordWithList> immutableProxy = ImmutableProxy.create(recordWithLists, ImmutableProxyOption.ALLOW_CLONING_RECORDS);

		assertThat(immutableProxy)
			.extracting(RecordWithList::values)
			.containsExactly(
				List.of("one", "two", "three"),
				List.of("five", "six", "seven")
			);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.get(1).values().add("two"))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.add(new RecordWithList(List.of())))
			.withMessage("This list is immutable");
	}

	@Test
	void testCreateImmutableProxyOfClassWithRecordHavingList_allowCloningOfRecords_setOfRecords() {
		Set<RecordWithList> recordWithLists = new LinkedHashSet<>(List.of(
			new RecordWithList(new ArrayList<>(List.of("one", "two", "three"))),
			new RecordWithList(new ArrayList<>(List.of("five", "six", "seven")))
		));

		Set<RecordWithList> immutableProxy = ImmutableProxy.create(recordWithLists, ImmutableProxyOption.ALLOW_CLONING_RECORDS);

		assertThat(immutableProxy)
			.extracting(RecordWithList::values)
			.containsExactly(
				List.of("one", "two", "three"),
				List.of("five", "six", "seven")
			);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.iterator().next().values().add("two"))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.add(new RecordWithList(List.of())))
			.withMessage("This set is immutable");
	}

	@Test
	void testCreateImmutableProxyOfClassWithRecordHavingList_allowCloningOfRecords_mapOfRecords() {
		Map<String, RecordWithList> recordWithLists = Map.of(
			"a", new RecordWithList(new ArrayList<>(List.of("one", "two", "three"))),
			"b", new RecordWithList(new ArrayList<>(List.of("five", "six", "seven"))));

		Map<String, RecordWithList> immutableProxy = ImmutableProxy.create(recordWithLists, ImmutableProxyOption.ALLOW_CLONING_RECORDS);

		assertThat(immutableProxy.values())
			.extracting(RecordWithList::values)
			.containsExactlyInAnyOrder(
				List.of("one", "two", "three"),
				List.of("five", "six", "seven")
			);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.get("a").values().add("two"))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.put("c", new RecordWithList(List.of())))
			.withMessage("This map is immutable");
	}

	@Test
	void testCreateImmutableProxyOfClassWithRecordHavingList_allowCloningOfRecords_object() {
		Map<String, RecordWithList> recordWithLists = Map.of(
			"a", new RecordWithList(new ArrayList<>(List.of("one", "two", "three"))),
			"b", new RecordWithList(new ArrayList<>(List.of("five", "six", "seven"))));

		@SuppressWarnings("unchecked")
		Map<String, RecordWithList> immutableProxy = (Map<String, RecordWithList>) ImmutableProxy.create((Object) recordWithLists, ImmutableProxyOption.ALLOW_CLONING_RECORDS);

		assertThat(immutableProxy.values())
			.extracting(RecordWithList::values)
			.containsExactlyInAnyOrder(
				List.of("one", "two", "three"),
				List.of("five", "six", "seven")
			);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.get("a").values().add("two"))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.put("c", new RecordWithList(List.of())))
			.withMessage("This map is immutable");
	}

	@Test
	void testCreateImmutableProxyOfClassWithRecordHavingList_allowCloningOfRecords_listOfListsOfRecords() {
		List<List<RecordWithList>> recordWithLists = List.of(
			List.of(
				new RecordWithList(new ArrayList<>(List.of("one", "two", "three"))),
				new RecordWithList(new ArrayList<>(List.of("five", "six", "seven"))))
		);

		List<List<RecordWithList>> immutableProxy = ImmutableProxy.create(recordWithLists, ImmutableProxyOption.ALLOW_CLONING_RECORDS);

		assertThat(immutableProxy)
			.flatMap(Function.identity())
			.extracting(RecordWithList::values)
			.containsExactly(
				List.of("one", "two", "three"),
				List.of("five", "six", "seven")
			);

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.get(0).get(1).values().add("two"))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.get(0).add(new RecordWithList(List.of())))
			.withMessage("This list is immutable");
	}

	@Test
	void testCreateImmutableProxyOfClassWithRecordHavingList_allowCloningOfRecords_listOfRecordsInRecord() {
		record HavingListOfRecordsWithList(List<RecordWithList> recordWithLists) {
		}

		HavingListOfRecordsWithList objectWithRecordsWithLists = new HavingListOfRecordsWithList(
			List.of(
				new RecordWithList(new ArrayList<>(List.of("one", "two", "three"))),
				new RecordWithList(new ArrayList<>(List.of("five", "six", "seven"))))
		);

		HavingListOfRecordsWithList immutableProxy = ImmutableProxy.create(objectWithRecordsWithLists, ImmutableProxyOption.ALLOW_CLONING_RECORDS);

		assertThat(immutableProxy)
			.extracting(HavingListOfRecordsWithList::recordWithLists)
			.isEqualTo(List.of(
				new RecordWithList(List.of("one", "two", "three")),
				new RecordWithList(List.of("five", "six", "seven"))));

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.recordWithLists().get(0).values().add("two"))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.recordWithLists().add(new RecordWithList(List.of())))
			.withMessage("This list is immutable");
	}

	@Test
	void testCreateImmutableProxyOfClassWithRecordHavingList_allowCloningOfRecords_listOfRecordsInBean() {
		TestBeanWithListOfRecordHavingList testBean = new TestBeanWithListOfRecordHavingList();
		testBean.setRecordsWithList(
			List.of(
				new RecordWithList(new ArrayList<>(List.of("one", "two", "three"))),
				new RecordWithList(new ArrayList<>(List.of("five", "six", "seven"))))
		);

		TestBeanWithListOfRecordHavingList immutableProxy = ImmutableProxy.create(testBean, ImmutableProxyOption.ALLOW_CLONING_RECORDS);

		assertThat(immutableProxy)
			.extracting(TestBeanWithListOfRecordHavingList::getRecordsWithList)
			.isEqualTo(List.of(
				new RecordWithList(List.of("one", "two", "three")),
				new RecordWithList(List.of("five", "six", "seven"))));

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.getRecordsWithList().get(0).values().add("two"))
			.withMessage("This list is immutable");

		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> immutableProxy.getRecordsWithList().add(new RecordWithList(List.of())))
			.withMessage("This list is immutable");
	}

	@Test
	void testCreateImmutableProxyOfClassWithRecordHavingAtomicLong() {
		TestBeanWithRecordHavingAtomicLong bean = new TestBeanWithRecordHavingAtomicLong();
		bean.setRecordWithAtomicLong(new RecordWithAtomicLong(new AtomicLong(1L)));

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> ImmutableProxy.create(bean).getRecordWithAtomicLong())
			.withMessage("class de.cronn.reflection.util.testclasses.RecordWithAtomicLong is a record that" +
						 " potentially contains mutable components." +
						 " Consider using ImmutableProxy.create(bean, ImmutableProxyOption.ALLOW_CLONING_RECORDS)" +
						 " to enable cloning of such records.");

		TestBeanWithRecordHavingAtomicLong immutableProxy = ImmutableProxy.create(bean, ImmutableProxyOption.ALLOW_CLONING_RECORDS);

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(immutableProxy::getRecordWithAtomicLong)
			.withMessage("Cannot create an immutable proxy for class java.util.concurrent.atomic.AtomicLong." +
						 " Method public final long java.util.concurrent.atomic.AtomicLong.get() is final.");
	}
}
