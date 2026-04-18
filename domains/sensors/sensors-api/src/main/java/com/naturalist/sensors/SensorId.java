package com.naturalist.sensors;

import com.naturalist.ddd.PersistenceId;

/**
 * Surrogate identity for a {@link Sensor} catalog entity.
 * <p>
 * {@code SensorId} is the intra-domain numeric surrogate for sensor catalog entries.
 * Cross-domain references to sensors (e.g. from the sensor analysis domain) use
 * {@link SensorName} — the stable slug — per ADR-001.
 */
public final class SensorId extends PersistenceId<Long> {

    private SensorId(Long value) {
        super(value);
    }

    public static SensorId of(Long value) {
        return new SensorId(value);
    }
}
