package com.naturalist.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

/** Surrogate UUIDv7 identity for an {@link OrganismObservation}. */
public final class OrganismObservationId extends EntityId {
    private OrganismObservationId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static OrganismObservationId of(UUID value) {
        return new OrganismObservationId(value);
    }

    public static OrganismObservationId create() {
        return new OrganismObservationId(EntityId.newUUID());
    }
}
