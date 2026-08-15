package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

/**
 * Surrogate identity of a {@code ReportedOptimum}. Its logical key is
 * {@code (nutrientName, labAnalysisId)} — one optimum per nutrient per analysis, the same grain as
 * {@code NutrientReading} — enforced as a unique constraint, with this surrogate as the physical
 * key.
 */
public final class ReportedOptimumId extends EntityId {

    private ReportedOptimumId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static ReportedOptimumId of(UUID value) {
        return new ReportedOptimumId(value);
    }

    public static ReportedOptimumId create() {
        return new ReportedOptimumId(EntityId.newUUID());
    }
}
