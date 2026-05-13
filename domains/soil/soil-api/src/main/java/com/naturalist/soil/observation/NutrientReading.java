package com.naturalist.soil.observation;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * A single measured nutrient value paired with its agronomic status classification.
 * <p>
 * {@code NutrientReading} is a component of {@link NutrientPanel} — it captures both
 * the raw measured value (in lbs/1000 sqft unless otherwise documented) and the
 * lab's status classification relative to the optimum range for the crop context.
 * <p>
 * The status is assigned at lab analysis time (or on import from the FGL report)
 * and is stored as a first-class field rather than computed on the fly, because
 * the optimum ranges are crop- and context-specific and the lab's assignment is the
 * authoritative interpretation.
 * <p>
 * Units: the FGL report expresses most macro- and micronutrients in lbs per 1000 sqft.
 * Dimensionless or differently-unitised parameters (pH, EC, limestone%, saturation%)
 * are carried directly on {@link NutrientPanel} rather than as {@code NutrientReading}
 * instances, since they have no meaningful "lbs/1000sqft" interpretation.
 */
public record NutrientReading(
        BigDecimal value,
        NutrientStatus status
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(value, "value")
                .notNull(status, "status");
    }
}
