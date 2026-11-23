package com.naturalist.insects;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class InsectFeatureId extends EntityId {
    private InsectFeatureId(UUID value) {
        super(value);
    }

    public static InsectFeatureId of(UUID value) {
        return new InsectFeatureId(value);
    }

    public static InsectFeatureId create() {
        return new InsectFeatureId(EntityId.newUUID());
    }
}