package de.cronn.reflection.util.testclasses;

import java.time.LocalDate;

public record RecordWithSealedClasses(String value,
									  SealedClass sealedClass,
									  SealedAbstractClass sealedAbstractClass,
									  SealedInterfaceWithTwoRecords sealedInterfaceWithTwoRecords,
									  RecursiveSealedInterface recursiveSealedInterface,
									  SealedInterfaceWithClassAndRecord sealedInterfaceWithClassAndRecord) {

	public sealed static class SealedClass {
		public static final class SubClass extends SealedClass {
		}
	}

	public sealed static abstract class SealedAbstractClass {
		public static final class SubClass extends SealedAbstractClass {
		}
	}

	public sealed interface SealedInterfaceWithTwoRecords {
		record Record1(String name) implements SealedInterfaceWithTwoRecords {
		}

		record Record2(String name, LocalDate dateOfBirth) implements SealedInterfaceWithTwoRecords {
		}
	}

	public sealed interface RecursiveSealedInterface {
		sealed interface RecursiveSealedInterfaceLevel1 extends RecursiveSealedInterface {
			record Record1(String name) implements RecursiveSealedInterfaceLevel1 {
			}
		}
	}

	public sealed interface SealedInterfaceWithClassAndRecord {
		final class FinalClassWithoutDefaultConstructor implements SealedInterfaceWithClassAndRecord {
			private FinalClassWithoutDefaultConstructor(String value) {
			}
		}

		record SomeRecord(int data) implements SealedInterfaceWithClassAndRecord {
		}
	}
}
