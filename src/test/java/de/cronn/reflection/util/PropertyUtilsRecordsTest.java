package de.cronn.reflection.util;

import static org.assertj.core.api.Assertions.*;

import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import de.cronn.reflection.util.testclasses.Point;
import de.cronn.reflection.util.testclasses.RecordWithSealedClasses;

class PropertyUtilsRecordsTest {

	@Test
	void testGetPropertyDescriptorsOfRecord() throws Exception {
		record TestRecord(int a, int b) {
		}

		List<String> propertyNames = collectPropertyNames(PropertyUtils.getPropertyDescriptors(TestRecord.class));
		assertThat(propertyNames).containsExactly(
			"a",
			"b",
			"class"
		);
	}

	@Test
	void testGetPropertyDescriptorsOfRecordImplementingAnInterface() throws Exception {
		interface HavingId {
			int getId();
		}
		record TestRecord(int a, int b) implements HavingId {
			@Override
			public int getId() {
				return a;
			}
		}

		List<String> propertyNames = collectPropertyNames(PropertyUtils.getPropertyDescriptors(TestRecord.class));
		assertThat(propertyNames).containsExactly(
			"a",
			"b",
			"class",
			"id"
		);
	}

	@Test
	void testGetPropertyName_multipleBooleanPrimitives() throws Exception {
		record TestRecord(boolean bool1, boolean bool2, boolean bool3, boolean bool4, boolean bool5) {
		}

		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::bool1)).isEqualTo("bool1");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::bool2)).isEqualTo("bool2");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::bool3)).isEqualTo("bool3");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::bool4)).isEqualTo("bool4");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::bool5)).isEqualTo("bool5");
	}

	@Test
	void testGetPropertyName_primitives() throws Exception {
		record PrimitivesRecord(byte byte1, byte byte2,
								short short1, short short2,
								int int1, int int2,
								long long1, long long2,
								float float1, float float2,
								double double1, double double2,
								char char1, char char2,
								String string1, String string2,
								boolean bool1, boolean bool2) {
		}

		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::byte1)).isEqualTo("byte1");
		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::byte2)).isEqualTo("byte2");

		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::short1)).isEqualTo("short1");
		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::short2)).isEqualTo("short2");

		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::int1)).isEqualTo("int1");
		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::int2)).isEqualTo("int2");

		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::long1)).isEqualTo("long1");
		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::long2)).isEqualTo("long2");

		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::float1)).isEqualTo("float1");
		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::float2)).isEqualTo("float2");

		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::double1)).isEqualTo("double1");
		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::double2)).isEqualTo("double2");

		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::char1)).isEqualTo("char1");
		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::char2)).isEqualTo("char2");

		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::string1)).isEqualTo("string1");
		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::string2)).isEqualTo("string2");

		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::bool1)).isEqualTo("bool1");
		assertThat(PropertyUtils.getPropertyName(PrimitivesRecord.class, PrimitivesRecord::bool2)).isEqualTo("bool2");
	}

	@Test
	void testGetPropertyName_recordInOtherPackage() throws Exception {
		assertThat(Point.class.getPackage()).isNotEqualTo(getClass().getPackage());
		assertThat(PropertyUtils.getPropertyName(Point.class, Point::x)).isEqualTo("x");
	}

	@Test
	void testGetPropertyName_recordWithObjects() throws Exception {
		record TestRecord(Set<String> set1, Set<String> set2,
						  Number number1, Number number2,
						  AbstractList<?> abstractList1, AbstractList<?> abstractList2,
						  List<String> list1, List<String> list2,
						  BigDecimal bigDecimal) {
		}

		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::set1)).isEqualTo("set1");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::set2)).isEqualTo("set2");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::list1)).isEqualTo("list1");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::list2)).isEqualTo("list2");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::abstractList1)).isEqualTo("abstractList1");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::abstractList2)).isEqualTo("abstractList2");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::bigDecimal)).isEqualTo("bigDecimal");
	}

	@Test
	void testGetPropertyName_recordWithBoxedPrimitives() throws Exception {
		record TestRecord(Boolean boxedBoolean1, Boolean boxedBoolean2,
						  Byte boxedByte1, Byte boxedByte2,
						  Short boxedShort1, Short boxedShort2,
						  Integer boxedInteger1, Integer boxedInteger2,
						  Long boxedLong1, Long boxedLong2,
						  Float boxedFloat1, Float boxedFloat2,
						  Double boxedDouble1, Double boxedDouble2,
						  Character boxedCharacter1, Character boxedCharacter2) {
		}

		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedBoolean1)).isEqualTo("boxedBoolean1");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedBoolean2)).isEqualTo("boxedBoolean2");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedByte1)).isEqualTo("boxedByte1");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedByte2)).isEqualTo("boxedByte2");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedShort1)).isEqualTo("boxedShort1");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedShort2)).isEqualTo("boxedShort2");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedInteger1)).isEqualTo("boxedInteger1");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedInteger2)).isEqualTo("boxedInteger2");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedLong1)).isEqualTo("boxedLong1");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedLong2)).isEqualTo("boxedLong2");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedFloat1)).isEqualTo("boxedFloat1");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedFloat2)).isEqualTo("boxedFloat2");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedDouble1)).isEqualTo("boxedDouble1");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedDouble2)).isEqualTo("boxedDouble2");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedCharacter1)).isEqualTo("boxedCharacter1");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::boxedCharacter2)).isEqualTo("boxedCharacter2");
	}

	@Test
	void testGetPropertyName_recordWithNestedRecords() throws Exception {
		record NestedRecord(int a, int b) {
		}

		record TestRecord(NestedRecord nestedRecord1, NestedRecord nestedRecord2) {
		}

		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::nestedRecord1)).isEqualTo("nestedRecord1");
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::nestedRecord2)).isEqualTo("nestedRecord2");
	}

	@Test
	void testGetPropertyName_noComponentAccessor() throws Exception {
		record TestRecord(int a) {
		}

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> PropertyUtils.getPropertyName(TestRecord.class, TestRecord::toString))
			.withMessage("Failed to find a component in " + TestRecord.class.getName() + " for the given component accessor.");
	}

	@Test
	void testGetPropertyName_nonComponentMethodAccessingComponentValue() throws Exception {
		record TestRecord(int a, int b) {
			int c() {
				return a;
			}
		}

		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::c)).isEqualTo("a");
	}

	@Test
	void testGetPropertyName_exceptionInMethod() throws Exception {
		record TestRecord(int a) {
			String throwException() {
				throw new RuntimeException("some exception");
			}
		}

		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(() -> PropertyUtils.getPropertyName(TestRecord.class, TestRecord::throwException))
			.withMessage("some exception");
	}

	@Test
	void testGetPropertyName_recordWithSecondConstructor() throws Exception {
		record TestRecord(int a, int b) {
			public TestRecord(int value) {
				this(value, 0);
			}
		}

		assertThat(PropertyUtils.getPropertyName(TestRecord.class, TestRecord::a)).isEqualTo("a");
		assertThat(PropertyUtils.getPropertyName(new TestRecord(1), TestRecord::a)).isEqualTo("a");
	}

	@Test
	void testGetPropertyName_isCached() throws Exception {
		record TestRecord(int a) {
			static boolean shouldThrow;

			int getA() {
				if (shouldThrow) {
					throw new RuntimeException("some exception");
				}
				return a();
			}
		}

		PropertyGetter<TestRecord> getter = TestRecord::getA;

		TestRecord.shouldThrow = false;
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, getter)).isEqualTo("a");

		TestRecord.shouldThrow = true;
		// Should throw neither, since the result must be cached
		assertThat(PropertyUtils.getPropertyName(TestRecord.class, getter)).isEqualTo("a");

		PropertyUtils.removeClassFromCache(TestRecord.class);

		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(() -> PropertyUtils.getPropertyName(TestRecord.class, getter))
			.withMessage("some exception");
	}

	@Test
	void testGetPropertyName_CallSiteSpecificLambda() throws Exception {
		record TestRecord(int a) {
		}

		PropertyGetter<TestRecord> lambda = TestRecord::a;
		PropertyGetter<TestRecord> callSiteSpecificLambda = lambda::get;

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> PropertyUtils.getPropertyName(TestRecord.class, callSiteSpecificLambda))
			.withMessage(callSiteSpecificLambda + " is call site specific");
	}

	@Test
	void testGetPropertyDescriptor() throws Exception {
		record TestRecord(int a, int b) {
		}

		TestRecord record = new TestRecord(1, 2);

		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(record, TestRecord::b);
		assertThat(PropertyUtils.isWritable(propertyDescriptor)).isFalse();
		assertThat(PropertyUtils.isReadable(propertyDescriptor)).isTrue();
		assertThat(propertyDescriptor).hasToString("java.beans.PropertyDescriptor[name=b; propertyType=int;" +
												   " readMethod=public int " + TestRecord.class.getName() + ".b()]");

		int read = PropertyUtils.read(record, propertyDescriptor);
		assertThat(read).isEqualTo(2);
	}

	@Test
	void testGetPropertyDescriptorByName() throws Exception {
		record TestRecord(int a, int b) {
		}

		TestRecord record = new TestRecord(1, 2);

		PropertyDescriptor propertyDescriptor1 = PropertyUtils.getPropertyDescriptorByNameOrThrow(record, "b");
		PropertyDescriptor propertyDescriptor2 = PropertyUtils.getPropertyDescriptor(record, TestRecord::b);
		assertThat(propertyDescriptor1).isSameAs(propertyDescriptor2);
	}

	@Test
	void testGetQualifiedPropertyName() throws Exception {
		record TestRecord(int a, int b) {
		}

		assertThat(PropertyUtils.getQualifiedPropertyName(TestRecord.class, TestRecord::a)).isEqualTo("TestRecord.a");
		assertThat(PropertyUtils.getQualifiedPropertyName(TestRecord.class, TestRecord::b)).isEqualTo("TestRecord.b");
	}

	@Test
	void testIsCollectionType() throws Exception {
		record TestRecord(int a, List<?> b) {
		}

		assertThat(PropertyUtils.isCollectionType(PropertyUtils.getPropertyDescriptor(TestRecord.class, TestRecord::a))).isFalse();
		assertThat(PropertyUtils.isCollectionType(PropertyUtils.getPropertyDescriptor(TestRecord.class, TestRecord::b))).isTrue();
	}

	@Test
	void testGetPropertyDescriptorOfRecordWithSealedInterface() throws Exception {
		PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(RecordWithSealedClasses.class, RecordWithSealedClasses::sealedInterfaceWithTwoRecords);
		assertThat(propertyDescriptor.getName()).isEqualTo("sealedInterfaceWithTwoRecords");
	}

	private static List<String> collectPropertyNames(Collection<PropertyDescriptor> propertyDescriptors) {
		return propertyDescriptors.stream()
			.map(PropertyDescriptor::getName)
			.collect(Collectors.toList());
	}

}
