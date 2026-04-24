package com.naturalist.insects.lifestages;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The stage's role in its species's chemistry story (e.g. Battus philenor:
 * larva ACQUISITION, pupa RETENTION, adult EXPRESSION, egg MATERNAL_TRANSFER).
 * Cross-stage consistency enforced on the InsectSpecies aggregate root.
 */
public record StageChemistryRole(
        Role role,
        @Nullable String notes
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.notNull(this, StageChemistryRole::role, "role");
    }

    public enum Role {
        ACQUISITION,
        RETENTION,
        EXPRESSION,
        MATERNAL_TRANSFER
    }
}
