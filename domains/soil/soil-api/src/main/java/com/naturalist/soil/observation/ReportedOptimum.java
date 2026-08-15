package com.naturalist.soil.observation;

import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * The optimum range a lab printed for one nutrient on one analysis — the "Optimum Range" column
 * beside a {@link NutrientReading}, stored as its own append-only fact.
 * <p>
 * <b>Why this is not a component of {@link NutrientReading}.</b> A reading is what the instrument
 * measured; an optimum is what the lab believes good looks like for the crop the sample was
 * submitted under. They have different authorship, different stability, and different truth
 * conditions — re-run the same soil for a different crop and the readings are unchanged while the
 * optima move. Hanging the range off the measurement would fuse the two and is the one change the
 * soil domain most explicitly rejects.
 * <p>
 * <b>Why store it at all,</b> when a strategy could compute it? Because the printed values are the
 * golden master. FGL's exchangeable-cation ranges turn out to be crop-invariant base-saturation
 * targets projected through the sample's own CEC, and storing what the lab actually printed is
 * what allows a future {@code FglReplicaStrategy} to be tested against reality rather than against
 * its own assumptions. See {@code SoilProfileFactoryTest}'s golden-master case.
 * <p>
 * Identity is the surrogate {@link ReportedOptimumId}; the logical key
 * {@code (nutrientName, labAnalysisId)} is enforced as a unique constraint by the data source —
 * the same grain as {@code NutrientReading}, so the two line up one-for-one when a lab reports
 * both.
 */
public record ReportedOptimum(
        ReportedOptimumId id,
        NutrientName nutrientName,
        LabAnalysisId labAnalysisId,
        OptimumRange range,
        MeasurementUnit unit
) implements Entity<ReportedOptimumId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(nutrientName, "nutrientName")
                .entityId(labAnalysisId, "labAnalysisId")
                .valueObject(range, "range")
                .notNull(unit, "unit");
    }
}
