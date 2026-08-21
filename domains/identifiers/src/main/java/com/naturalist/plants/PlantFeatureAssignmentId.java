package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

/** Surrogate UUIDv7 identity for a {@code PlantFeatureAssignment}. */
public final class PlantFeatureAssignmentId extends EntityId {

    private PlantFeatureAssignmentId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static PlantFeatureAssignmentId of(UUID value) {
        return new PlantFeatureAssignmentId(value);
    }

    public static PlantFeatureAssignmentId create() {
        return new PlantFeatureAssignmentId(EntityId.newUUID());
    }
}
