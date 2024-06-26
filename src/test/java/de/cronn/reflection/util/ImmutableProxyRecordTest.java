package de.cronn.reflection.util;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import de.cronn.reflection.util.immutable.ImmutableProxy;
import de.cronn.reflection.util.immutable.ImmutableProxyOption;
import de.cronn.reflection.util.testclasses.Point;
import de.cronn.reflection.util.testclasses.RecordWithAtomicLong;
import de.cronn.reflection.util.testclasses.RecordWithList;
import de.cronn.reflection.util.testclasses.TestBeanWithRecordField;
import de.cronn.reflection.util.testclasses.TestBeanWithRecordHavingAtomicLong;
import de.cronn.reflection.util.testclasses.TestBeanWithRecordHavingList;

class ImmutableProxyRecordTest {

	@Test
	void testCreateImmutableProxyOfRecordWithPrimitives() throws Exception {
		record TestRecord(int a, int b, String string, boolean flag) {
		}

		TestRecord record = new TestRecord(1, 2, "data", true);
		TestRecord proxy = ImmutableProxy.create(record);
		assertThat(proxy).isSameAs(record);
	}

	@Test
	void testCreateImmutableProxyOfClassWithRecord() throws Exception {
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
	void testCreateImmutableProxyOfClassWithRecordHavingList() throws Exception {
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
	void testCreateImmutableProxyOfClassWithRecordHavingList_allowCloningOfRecords() throws Exception {
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
	void testCreateImmutableProxyOfClassWithRecordHavingAtomicLong() throws Exception {
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
