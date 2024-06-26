package de.cronn.reflection.util;

import java.lang.reflect.Constructor;
import java.util.function.Function;

import de.cronn.reflection.util.immutable.ImmutableProxy;

public final class RecordUtils {

	private static final ClassValue<Boolean> recordImmutabilityCache = ClassValues.create(recordClass -> {
		return RecordSupport.getRecordComponents(recordClass)
			.allMatch(recordComponent -> ImmutableProxy.isImmutable(recordComponent.getType()));
	});

	private RecordUtils() {
	}

	public static boolean hasOnlyImmutableRecordComponents(Class<?> recordClass) {
		return recordImmutabilityCache.get(recordClass);
	}

	public static <T> T cloneRecord(T record, Function<Object, Object> valueMapping) {
		Object[] values = RecordSupport.getRecordComponents(record.getClass())
			.map(recordComponent -> {
				try {
					Object value = recordComponent.getAccessor().invoke(record);
					return valueMapping.apply(value);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			})
			.toArray(Object[]::new);
		try {
			Constructor<T> recordConstructor = RecordSupport.getRecordConstructor(ClassUtils.getRealClass(record));
			return ClassUtils.createInstance(recordConstructor, values);
		} catch (ReflectiveOperationException e) {
			throw new ReflectionRuntimeException(e);
		}
	}
}
