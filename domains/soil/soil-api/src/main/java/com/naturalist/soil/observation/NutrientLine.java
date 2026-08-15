package com.naturalist.soil.observation;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * One row of a lab report as a reader should see it: the nutrient, what was measured, what the lab
 * printed as optimum, and how the two compare. A presentation projection — it introduces no facts,
 * it only puts already-stored ones on the same line.
 * <p>
 * <b>Why this exists rather than template logic.</b> The rules that keep this page honest —
 * absent is not zero, no band positions, every assessment attributed — are domain rules, and a
 * template is the worst place to keep them. Each is enforced here by construction: the reading and
 * the optimum are {@link Optional}, the comparison is a three-way position that carries its
 * {@link AssessmentSource}, and there is nowhere to put a band. A template rendering a
 * {@code NutrientLine} cannot break the rules by omission.
 * <p>
 * Both {@code reading} and {@code optimum} are independently optional, and all four combinations
 * are real: a lab may report a value with no optimum, an optimum with no value (a row it ran no
 * test for), both, or neither.
 */
public record NutrientLine(
        NutrientName nutrientName,
        Optional<NutrientReading> reading,
        Optional<ReportedOptimum> optimum,
        OptimumComparison comparison
) implements ReadModel {

    public NutrientLine {
        reading = reading == null ? Optional.empty() : reading;
        optimum = optimum == null ? Optional.empty() : optimum;
        comparison = comparison == null ? OptimumComparison.notComparable() : comparison;
    }

    /** Pairs a nutrient's measurement with its printed optimum and compares them. */
    public static NutrientLine of(NutrientName nutrientName,
                                  Optional<NutrientReading> reading,
                                  Optional<ReportedOptimum> optimum) {
        return new NutrientLine(nutrientName, reading, optimum,
                OptimumComparison.of(reading, optimum));
    }

    /** Whether the lab reported a value for this nutrient at all. */
    public boolean wasMeasured() {
        return reading.isPresent();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(nutrientName, "nutrientName")
                .valueObject(comparison, "comparison")
                .namedEntityOrNull(reading.orElse(null), "reading")
                .namedEntityOrNull(optimum.orElse(null), "optimum");
    }
}
