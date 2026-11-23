package com.naturalist.insects;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class InsectObservationId extends EntityId {
    private InsectObservationId(UUID value) {
        super(value);
    }

    public static InsectObservationId of(UUID value) {
        return new InsectObservationId(value);
    }

    public static InsectObservationId create() {
        return new InsectObservationId(EntityId.newUUID());
    }
}
