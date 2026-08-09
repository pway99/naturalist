package com.naturalist.soil.observation;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * The cation base-saturation profile of a single soil sample — how the sample's cation
 * exchange capacity (CEC) is occupied across the exchangeable cations, each expressed as a
 * percentage of total CEC.
 * <p>
 * {@code CationBaseSaturation} is a component of {@link NutrientPanel} and reports the FGL
 * "% Base Saturation" block: calcium, magnesium, potassium, sodium, and hydrogen. The five
 * percentages are a single ubiquitous-language concept — they partition the exchange sites
 * and sum to approximately 100% — and are therefore modelled as one cohesive value object
 * rather than five loose fields (ADR-013).
 * <p>
 * <b>Not to be confused with</b> {@link NutrientPanel#saturationPct()}, which is the physical
 * water/paste saturation of the sample (a drainage indicator), a distinct measurement.
 * <p>
 * <b>Oak Vista reference — Box 1 (CH 2671853-001, sampled March 3, 2026):</b>
 * Ca 74.6%, Mg 22.9%, K 2.09%, Na 0.408%, H 1.00%. The Ca-dominant, Mg-elevated balance
 * with negligible sodium and hydrogen is characteristic of the worm-casting/coco-coir blend
 * under gypsum rehabilitation.
 */
public record CationBaseSaturation(
        BigDecimal calciumPct,
        BigDecimal magnesiumPct,
        BigDecimal potassiumPct,
        BigDecimal sodiumPct,
        BigDecimal hydrogenPct
) implements ValueObject {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(calciumPct, "calciumPct")
                .inRange(calciumPct, BigDecimal.ZERO, HUNDRED, "calciumPct")
                .notNull(magnesiumPct, "magnesiumPct")
                .inRange(magnesiumPct, BigDecimal.ZERO, HUNDRED, "magnesiumPct")
                .notNull(potassiumPct, "potassiumPct")
                .inRange(potassiumPct, BigDecimal.ZERO, HUNDRED, "potassiumPct")
                .notNull(sodiumPct, "sodiumPct")
                .inRange(sodiumPct, BigDecimal.ZERO, HUNDRED, "sodiumPct")
                .notNull(hydrogenPct, "hydrogenPct")
                .inRange(hydrogenPct, BigDecimal.ZERO, HUNDRED, "hydrogenPct");
    }
}
