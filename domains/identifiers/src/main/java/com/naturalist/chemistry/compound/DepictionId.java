package com.naturalist.chemistry.compound;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class DepictionId extends EntityId {
    private DepictionId(UUID value) {
        super(value);
    }

    public static DepictionId of(UUID value) {
        return new DepictionId(value);
    }

    public static DepictionId create() {
        return new DepictionId(EntityId.newUUID());
    }
}
