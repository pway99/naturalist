package com.naturalist.sensors;

import com.naturalist.ddd.PersistenceId;

/**
 * Surrogate identity for a {@link SensorReading} fact entity.
 * <p>
 * {@code SensorReadingId} is the sole identity carrier for sensor readings —
 * per ADR-005, {@code FactEntity} instances carry no {@code EntityName}.
 * Sensor readings are never referenced cross-domain by slug; they are accessed
 * through temporal queries on the soil domain's service interfaces.
 */
public final class SensorReadingId extends PersistenceId<Long> {

    private SensorReadingId(Long value) {
        super(value);
    }

    public static SensorReadingId of(Long value) {
        return new SensorReadingId(value);
    }
}
