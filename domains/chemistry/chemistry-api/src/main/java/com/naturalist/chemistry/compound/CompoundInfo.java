package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.element.PeriodicElement;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Chemical classification facts for a compound — what it is, not who it is.
 * <p>
 * {@code CompoundInfo} is a {@link ValueObject} owned by {@link Compound}. It groups the
 * physical-chemical identity fields that define a compound's classification: molecular
 * formula, weight, type, pH character, and constituent elements.
 * <p>
 * Identity (slug, common name, persistence key) lives on {@link Compound}.
 * Profile data (solubility, bioavailability, etc.) lives on {@link Compound}.
 * {@code CompoundInfo} carries only the classification facts.
 */
public record CompoundInfo(
        String formula,
        @Nullable MolecularWeight molecularWeight,
        CompoundType type,
        PhCharacter phCharacter,
        Set<PeriodicElement> constituentElements
) implements ValueObject {

    public boolean isPHNeutral() {
        return phCharacter == PhCharacter.NEUTRAL;
    }

    public boolean isAcidic() {
        return phCharacter == PhCharacter.ACIDIC ||
                phCharacter == PhCharacter.STRONGLY_ACIDIC;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(this, CompoundInfo::formula, "formula")
                .notNull(this, CompoundInfo::type, "type")
                .notNull(this, CompoundInfo::phCharacter, "phCharacter")
                .notNull(this, CompoundInfo::constituentElements, "constituentElements");
    }
}
