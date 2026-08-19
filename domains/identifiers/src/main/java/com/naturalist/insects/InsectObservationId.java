package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class InsectObservationId extends EntityId {
    private InsectObservationId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static InsectObservationId of(UUID value) {
        return new InsectObservationId(value);
    }

    public static InsectObservationId create() {
        return new InsectObservationId(EntityId.newUUID());
    }
}
