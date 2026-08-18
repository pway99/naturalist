package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

/** Surrogate UUIDv7 identity for a plant {@code FieldObservation}. */
public final class FieldObservationId extends EntityId {

    private FieldObservationId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static FieldObservationId of(UUID value) {
        return new FieldObservationId(value);
    }

    public static FieldObservationId create() {
        return new FieldObservationId(EntityId.newUUID());
    }
}
