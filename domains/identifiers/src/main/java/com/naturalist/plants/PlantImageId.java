package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

/** Surrogate UUIDv7 identity for a {@code PlantImage}. */
public final class PlantImageId extends EntityId {

    private PlantImageId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static PlantImageId of(UUID value) {
        return new PlantImageId(value);
    }

    public static PlantImageId create() {
        return new PlantImageId(EntityId.newUUID());
    }
}
