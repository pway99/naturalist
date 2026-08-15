package com.naturalist.garden;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

/**
 * Surrogate identity of a {@code Planting} — one crop, in one place, over one period.
 * <p>
 * Surrogate rather than a slug because a planting has no stable natural name: the same crop
 * returns to the same bed season after season, and {@code tomato-box-1} would collide with
 * itself every year.
 * <p>
 * Deliberately not {@code PlantId} — that name belongs to the plants domain, which identifies
 * botanical species rather than acts of cultivation.
 */
public final class PlantingId extends EntityId {

    private PlantingId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static PlantingId of(UUID value) {
        return new PlantingId(value);
    }

    public static PlantingId create() {
        return new PlantingId(EntityId.newUUID());
    }
}
