package com.naturalist.sensors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.FactName;

import java.util.UUID;

/**
 * Globally unique identity for a {@link SensorReading} fact entity.
 * <p>
 * Client-generated at the moment of measurement — before any persistence interaction.
 * Serves as the idempotency key: a reading submitted multiple times (network retry,
 * offline sync) carries the same UUID and will be de-duplicated by the unique
 * constraint in {@code TestEntitySource} and the RDBMS unique index.
 */
public final class SensorReadingName extends FactName {

    private SensorReadingName(UUID value) {
        super(value);
    }

    @JsonCreator
    public static SensorReadingName of(UUID value) {
        return new SensorReadingName(value);
    }
}
