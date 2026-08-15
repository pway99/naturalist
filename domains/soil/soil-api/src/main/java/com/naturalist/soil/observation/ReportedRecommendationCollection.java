package com.naturalist.soil.observation;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Multi-result return type for {@link ReportedRecommendation} queries (ADR-011) — an analysis's
 * recommendation rows, or one input's recommendations across analyses over time.
 */
public final class ReportedRecommendationCollection extends BehavioralCollection<ReportedRecommendation> {

    ReportedRecommendationCollection(Collection<ReportedRecommendation> recommendations) {
        super(recommendations);
    }

    public static ReportedRecommendationCollection of(Collection<ReportedRecommendation> recommendations) {
        return new ReportedRecommendationCollection(recommendations);
    }

    public static ReportedRecommendationCollection empty() {
        return new ReportedRecommendationCollection(List.of());
    }

    /**
     * The row printed for one input, if this collection carries it. Empty means the report had no
     * such row — distinct from a row printed {@code None}, which is the lab actively recommending
     * nothing.
     */
    public Optional<ReportedRecommendation> forInput(RecommendedInputName inputName) {
        return stream().filter(r -> r.inputName().equals(inputName)).findFirst();
    }

    /**
     * Only the rows recommending an actual application — the short list worth acting on. On both
     * March 2026 reports this is three rows of twelve.
     */
    public ReportedRecommendationCollection applicationsOnly() {
        return new ReportedRecommendationCollection(
                stream().filter(ReportedRecommendation::recommendsApplication).toList());
    }
}
