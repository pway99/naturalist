package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Structured, query-addressable functional ecology for an insect catalog
 * record at any rank — family, genus, species, or subspecies. The role
 * record exists once per organism (uniqueness enforced on
 * {@code parentName}); the organism's full set of ecological roles is
 * expressed as the {@code guilds} component.
 * <p>
 * {@code parentName} carries the typed slug of the catalog record this
 * role attaches to. The sealed {@link InsectRankName} marker statically
 * constrains the slot to the four insect-side rank names; cross-domain
 * names cannot compile in. The same cross-rank pattern as
 * {@link InsectImage} — one consumer of the pattern was coincidence;
 * two consumers are a pattern.
 * <p>
 * {@code guilds} is the structured assignment of which
 * {@link FunctionalGuild} roles the organism fills at Oak Vista —
 * pollinator, predator, food-web participant, etc. At least one
 * guild is required: a role record without any guild assignment has
 * no purpose, since the whole point of the entity is to carry the
 * structured ecological role. "We haven't documented this organism's
 * ecology yet" is expressed by the absence of a role record, not by an
 * empty set on a present record.
 * <p>
 * {@code beneficial} is the high-level garden-management flag — true for
 * organisms whose presence is desirable. Conceptually paired with
 * {@code guilds} but not derivable from it: a {@code MIGRATORY} species
 * can be either beneficial or neutral, and {@code FOOD_WEB} participants
 * are usually neutral.
 * <p>
 * Jackson dispatch on {@code parentName} mirrors {@link InsectImage}:
 * field-level {@code @JsonTypeInfo} with {@link As#EXTERNAL_PROPERTY}
 * flattens the discriminator into a sibling {@code "parentRank"} field,
 * keeping {@code parentName} itself a plain slug string via
 * {@code EntityName}'s {@code @JsonValue}. The polymorphic envelope
 * applies only at opt-in consumer sites, never at direct leaf-class
 * serializations.
 */
public record InsectFunctionalRole(
        InsectFunctionalRoleId name,
        @JsonTypeInfo(use = Id.NAME, property = "parentRank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = InsectFamilyName.class, name = "FAMILY"),
                @Type(value = InsectGenusName.class, name = "GENUS"),
                @Type(value = InsectSpeciesName.class, name = "SPECIES"),
                @Type(value = InsectSubspeciesName.class, name = "SUBSPECIES")
        })
        InsectRankName parentName,
        Set<FunctionalGuild> guilds,
        boolean beneficial
) implements Entity<InsectFunctionalRoleId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(name, "name")
                .identifier(parentName, "parentName")
                .notEmpty(guilds, "guilds");
    }
}
