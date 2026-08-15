package com.naturalist.soil.observation;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Multi-result return type for {@link ReportedOptimum} queries (ADR-011) — an analysis's printed
 * optima, or one nutrient's optima across analyses over time.
 */
public final class ReportedOptimumCollection extends BehavioralCollection<ReportedOptimum> {

    ReportedOptimumCollection(Collection<ReportedOptimum> optima) {
        super(optima);
    }

    public static ReportedOptimumCollection of(Collection<ReportedOptimum> optima) {
        return new ReportedOptimumCollection(optima);
    }

    public static ReportedOptimumCollection empty() {
        return new ReportedOptimumCollection(List.of());
    }

    /**
     * The optimum printed for one nutrient, if this collection carries it. Empty means the lab
     * printed no optimum for that nutrient — distinct from {@link OptimumRange.NotApplicable},
     * which is the lab printing {@code ---} on a row that exists.
     */
    public Optional<ReportedOptimum> forNutrient(NutrientName nutrientName) {
        return stream().filter(o -> o.nutrientName().equals(nutrientName)).findFirst();
    }
}
