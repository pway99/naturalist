package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

/**
 * Surrogate identity of a {@code NutrientReading}. The reading's logical key is
 * {@code (nutrientName, labAnalysisId)}; this framework offers only single-slug or single-UUIDv7
 * identity, so the logical key is enforced as a unique constraint and this surrogate is the
 * physical key.
 */
public final class NutrientReadingId extends EntityId {

    private NutrientReadingId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static NutrientReadingId of(UUID value) {
        return new NutrientReadingId(value);
    }

    public static NutrientReadingId create() {
        return new NutrientReadingId(EntityId.newUUID());
    }
}
