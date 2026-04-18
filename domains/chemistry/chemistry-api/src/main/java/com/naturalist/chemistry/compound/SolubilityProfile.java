package com.naturalist.chemistry.compound;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Water solubility of a compound at standard conditions.
 * <p>
 * Owned inline by {@link CompoundInfo} as a value object. Solubility is an intrinsic
 * physical property of the compound — it has no identity separate from the compound
 * it describes.
 */
public record SolubilityProfile(
        Solubility gramsPerLiterAt20C,
        SolubilityCategory category,
        BigDecimal ecContributionFactor,
        String notes
) implements ValueObject {

    public boolean isHighlySoluble() {
        return gramsPerLiterAt20C.isHighlySoluble();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedValue(this, SolubilityProfile::gramsPerLiterAt20C, "gramsPerLiterAt20C")
                .notNull(this, SolubilityProfile::category, "category")
                .notNull(this, SolubilityProfile::ecContributionFactor, "ecContributionFactor")
                .notNull(this, SolubilityProfile::notes, "notes");
    }

    public enum SolubilityCategory {
        INSOLUBLE,          // < 0.1 g/L
        SPARINGLY_SOLUBLE,  // 0.1-2.4 g/L  — CaSO4 (2.4 g/L)
        SLIGHTLY_SOLUBLE,   // 2.4-10 g/L
        SOLUBLE,            // 10-100 g/L
        HIGHLY_SOLUBLE,     // > 100 g/L    — K2SO4 (120 g/L), MgSO4 (710 g/L)
        MISCIBLE            // fully miscible — formic acid, organic acids
    }
}
