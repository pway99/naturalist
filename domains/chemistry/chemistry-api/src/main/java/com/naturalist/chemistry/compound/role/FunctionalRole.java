package com.naturalist.chemistry.compound.role;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Functional role a compound plays in the field — what it <em>does</em>.
 * <p>
 * Sealed so individual roles can carry attached state if and when they need it.
 * Initial permits are stateless record types; treat them as the sealed-interface
 * equivalent of enum constants until a concrete role acquires data.
 * <p>
 * {@code FunctionalRole} extends {@link ValueObject} and provides a default no-op
 * {@link #invariants()} — stateless permits inherit it without ceremony, but the
 * validation pipeline is wired up now so promoting a permit to a stateful record
 * does not require chasing down validation call sites. {@code CompoundInfo}
 * validates {@code functionalRoles} via {@code valueObjectCollection(...)}, walking
 * whatever invariants the concrete permit declares — when a future permit overrides
 * the default, it is automatically picked up.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FunctionalRole.Chelator.class, name = "CHELATOR"),
        @JsonSubTypes.Type(value = FunctionalRole.Fumigant.class, name = "FUMIGANT"),
        @JsonSubTypes.Type(value = FunctionalRole.BiologicalCatalyst.class, name = "BIOLOGICAL_CATALYST"),
        @JsonSubTypes.Type(value = FunctionalRole.Fertilizer.class, name = "FERTILIZER"),
        @JsonSubTypes.Type(value = FunctionalRole.Acaricide.class, name = "ACARICIDE")
})
public sealed interface FunctionalRole extends ValueObject {

    /**
     * Default no-op {@code invariants()} — stateless permits have no constraints
     * of their own. Permits that acquire state override this to declare their
     * own validation; {@code CompoundInfo} already walks each element of
     * {@code functionalRoles} via {@code valueObjectCollection(...)}, so a future
     * stateful permit is picked up automatically without call-site updates.
     */
    @Override
    default Consumer<? extends Constraints> invariants() {
        return i -> {
        };
    }

    record Chelator() implements FunctionalRole {
    }

    record Fumigant() implements FunctionalRole {
    }

    record BiologicalCatalyst() implements FunctionalRole {
    }

    record Fertilizer() implements FunctionalRole {
    }

    record Acaricide() implements FunctionalRole {
    }
}
