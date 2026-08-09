package com.naturalist.soil.observation;

import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * A single nutrient measurement from a soil laboratory analysis — the fact grain of soil chemistry,
 * one reading per nutrient per analysis. Purely a measured fact: the {@code value} in its
 * {@code unit}, keyed by which nutrient and which analysis. It carries no optimum range or status —
 * those are interpretation, derived by applying a {@code CropProfile} in the interpretation effort,
 * not properties of the measurement.
 * <p>
 * Identity is the surrogate {@link NutrientReadingId}; the logical key
 * {@code (nutrientName, labAnalysisId)} is enforced as a unique constraint by the data source.
 */
public record NutrientReading(
        NutrientReadingId id,
        NutrientName nutrientName,
        LabAnalysisId labAnalysisId,
        BigDecimal value,
        MeasurementUnit unit
) implements Entity<NutrientReadingId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(nutrientName, "nutrientName")
                .entityId(labAnalysisId, "labAnalysisId")
                .notNull(value, "value")
                .notNull(unit, "unit");
    }
}
