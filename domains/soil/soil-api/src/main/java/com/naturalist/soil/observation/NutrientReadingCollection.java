package com.naturalist.soil.observation;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Multi-result return type for {@link NutrientReading} queries (ADR-011) — a panel's readings, or a
 * nutrient's readings across analyses over time.
 */
public final class NutrientReadingCollection extends BehavioralCollection<NutrientReading> {

    NutrientReadingCollection(Collection<NutrientReading> readings) {
        super(readings);
    }

    public static NutrientReadingCollection of(Collection<NutrientReading> readings) {
        return new NutrientReadingCollection(readings);
    }

    public static NutrientReadingCollection empty() {
        return new NutrientReadingCollection(List.of());
    }
}
