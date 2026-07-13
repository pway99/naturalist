package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

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
