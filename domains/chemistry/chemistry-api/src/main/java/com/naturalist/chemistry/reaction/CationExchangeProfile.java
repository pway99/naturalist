package com.naturalist.chemistry.reaction;

import com.naturalist.chemistry.element.ElementName;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Specialized profile for cation exchange reactions.
 * Particularly relevant to the Oak Vista coco coir rehabilitation program
 * where Ca2+ from gypsum displacement displaces K+ and Na+ from exchange sites.
 * <p>
 * The selectivity series governs which cations displace which.
 * Higher selectivity coefficient = stronger binding to exchange sites.
 * <p>
 * Agnostic to specific cations — values from reactions.json.
 */
public record CationExchangeProfile(
        ElementName displacingCation,
        ElementName displacedCation,
        BigDecimal selectivityCoefficient,
        BigDecimal exchangeCapacityCmolKg,
        String exchangeMaterial,
        String rehabilitationNotes
) implements ValueObject {

    /**
     * Displacement is thermodynamically favorable when the displacing cation
     * has higher selectivity than the displaced cation.
     * For divalent Ca2+ displacing monovalent K+: favorable at moderate concentrations.
     */
    public boolean isDisplacementFavorable() {
        return selectivityCoefficient.compareTo(BigDecimal.ONE) > 0;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(displacingCation, "displacingCation")
                .entityName(displacedCation, "displacedCation")
                .notNull(this, CationExchangeProfile::selectivityCoefficient, "selectivityCoefficient")
                .notNull(this, CationExchangeProfile::exchangeCapacityCmolKg, "exchangeCapacityCmolKg");
    }
}
