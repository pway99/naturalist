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
 * {@code CationBaseSaturation} is a component of {@link SoilPhysicalCharacteristics} and reports
 * the FGL "% Base Saturation" block: calcium, magnesium, potassium, sodium, and hydrogen. The five
 * percentages are a single ubiquitous-language concept — they partition the exchange sites
 * and sum to approximately 100% — and are therefore modelled as one cohesive value object
 * rather than five loose fields (ADR-013).
 * <p>
 * <b>Censored hydrogen.</b> FGL prints {@code CEC-Hydrogen < 1.00} rather than a number: the true
 * value is somewhere in {@code [0, 1.00)} and the instrument cannot say where. When
 * {@code hydrogenBelowDetectionLimit} is set, {@code hydrogenPct} is <em>the detection limit, not
 * the measurement</em>. Storing 1.00 as if it were measured overstates hydrogen by up to a full
 * point of CEC and makes the base-saturation sum look exact when it is not — hence
 * {@link #saturationSumLowerBound()} and {@link #saturationSumUpperBound()} rather than a single
 * sum.
 * <p>
 * This is deliberately a marker on the one component that needs it, not a general censored-value
 * type. A sealed {@code Quantity} covering below-limit, above-limit and estimated values is the
 * right shape once a second censored measurement actually appears; building it for one field
 * would be designing against an imagined requirement.
 * <p>
 * <b>Not to be confused with</b> {@link SoilPhysicalCharacteristics#saturationPct()}, which is the
 * physical water/paste saturation of the sample (a drainage indicator), a distinct measurement.
 * <p>
 * <b>Oak Vista reference — Box 1 (CH 2671853-001, sampled March 3, 2026):</b>
 * Ca 74.6%, Mg 22.9%, K 2.09%, Na 0.408%, H &lt; 1.00%. The Ca-dominant, Mg-elevated balance
 * with negligible sodium and hydrogen is characteristic of the worm-casting/coco-coir blend
 * under gypsum rehabilitation.
 */
public record CationBaseSaturation(
        BigDecimal calciumPct,
        BigDecimal magnesiumPct,
        BigDecimal potassiumPct,
        BigDecimal sodiumPct,
        BigDecimal hydrogenPct,
        boolean hydrogenBelowDetectionLimit
) implements ValueObject {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * The sum of the five percentages taking hydrogen at its largest possible value. Equal to
     * {@link #saturationSumLowerBound()} when hydrogen was actually measured.
     */
    public BigDecimal saturationSumUpperBound() {
        return calciumPct.add(magnesiumPct).add(potassiumPct).add(sodiumPct).add(hydrogenPct);
    }

    /**
     * The sum of the five percentages taking hydrogen at its smallest possible value — zero when
     * the reading is censored, since {@code < 1.00} excludes nothing above zero.
     */
    public BigDecimal saturationSumLowerBound() {
        return hydrogenBelowDetectionLimit
                ? calciumPct.add(magnesiumPct).add(potassiumPct).add(sodiumPct)
                : saturationSumUpperBound();
    }

    /** Whether the sum is a single number rather than a range — true when hydrogen was measured. */
    public boolean hasExactSaturationSum() {
        return !hydrogenBelowDetectionLimit;
    }

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
