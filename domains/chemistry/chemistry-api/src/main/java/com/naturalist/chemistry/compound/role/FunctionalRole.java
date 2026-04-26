package com.naturalist.chemistry.compound.role;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Functional role a compound plays in the field — what it <em>does</em>.
 * <p>
 * Sealed so individual roles can carry attached state if and when they need it.
 * Initial permits are stateless record types; treat them as the sealed-interface
 * equivalent of enum constants until a concrete role acquires data. If a role
 * acquires state and behavior it should be promoted to a {@code ValueObject} and
 * added to the {@code Compound} graph walk.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FunctionalRole.Chelator.class,           name = "CHELATOR"),
        @JsonSubTypes.Type(value = FunctionalRole.Fumigant.class,           name = "FUMIGANT"),
        @JsonSubTypes.Type(value = FunctionalRole.BiologicalCatalyst.class, name = "BIOLOGICAL_CATALYST"),
        @JsonSubTypes.Type(value = FunctionalRole.Fertilizer.class,         name = "FERTILIZER"),
        @JsonSubTypes.Type(value = FunctionalRole.Acaricide.class,          name = "ACARICIDE")
})
public sealed interface FunctionalRole {
    record Chelator()           implements FunctionalRole {}
    record Fumigant()           implements FunctionalRole {}
    record BiologicalCatalyst() implements FunctionalRole {}
    record Fertilizer()         implements FunctionalRole {}
    record Acaricide()          implements FunctionalRole {}
}
