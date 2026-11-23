package com.naturalist.plants;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

/** Surrogate UUIDv7 identity for a plant {@link com.naturalist.observation.OrganismObservation}. */
public final class PlantObservationId extends EntityId {

    private PlantObservationId(UUID value) {
        super(value);
    }

    public static PlantObservationId of(UUID value) {
        return new PlantObservationId(value);
    }

    public static PlantObservationId create() {
        return new PlantObservationId(EntityId.newUUID());
    }
}
