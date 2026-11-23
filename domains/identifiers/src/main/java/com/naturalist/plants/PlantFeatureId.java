package com.naturalist.plants;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

/** Surrogate UUIDv7 identity for a {@code PlantFeature}. */
public final class PlantFeatureId extends EntityId {

    private PlantFeatureId(UUID value) {
        super(value);
    }

    public static PlantFeatureId of(UUID value) {
        return new PlantFeatureId(value);
    }

    public static PlantFeatureId create() {
        return new PlantFeatureId(EntityId.newUUID());
    }
}
