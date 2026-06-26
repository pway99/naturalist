package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class InsectFeatureId extends EntityId {
    private InsectFeatureId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static InsectFeatureId of(UUID value) {
        return new InsectFeatureId(value);
    }

    public static InsectFeatureId create() {
        return new InsectFeatureId(EntityId.newUUID());
    }
}