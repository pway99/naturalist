package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

/**
 * Surrogate identity of a {@code ReportedRecommendation}. Its logical key is
 * {@code (inputName, labAnalysisId)} — one row per recommended input per analysis — enforced as a
 * unique constraint, with this surrogate as the physical key.
 */
public final class ReportedRecommendationId extends EntityId {

    private ReportedRecommendationId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static ReportedRecommendationId of(UUID value) {
        return new ReportedRecommendationId(value);
    }

    public static ReportedRecommendationId create() {
        return new ReportedRecommendationId(EntityId.newUUID());
    }
}
