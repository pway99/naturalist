package com.naturalist.insects;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class InsectFeatureAssignmentId extends EntityId {
    private InsectFeatureAssignmentId(UUID value) {
        super(value);
    }

    public static InsectFeatureAssignmentId of(UUID value) {
        return new InsectFeatureAssignmentId(value);
    }

    public static InsectFeatureAssignmentId create() {
        return new InsectFeatureAssignmentId(EntityId.newUUID());
    }
}