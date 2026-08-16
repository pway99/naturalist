package com.naturalist.plants;

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
 * What a plant taxon <em>does</em> at Oak Vista — the ecological and horticultural
 * functions it fills, recorded at whatever rank the observation supports.
 * <p>
 * {@code plantName} is a {@link PlantRankName}, so a role can attach to a family, a genus
 * or a species. That is the point: a bed of unidentified salvia still supports pollinators,
 * and the citrus trees are a food crop whether or not anyone has keyed them to species.
 * Duplicating a {@code roles} component onto every rank record instead would put the same
 * concern in three places and leave genus-rank taxa unable to carry it at all.
 * <p>
 * <b>Why this is not a component on the rank records.</b> Insects made exactly this move
 * in PL-11 — functional ecology came off {@code InsectSpecies} onto the cross-rank
 * {@link com.naturalist.insects.InsectFunctionalRole}, for the same reason. This record is
 * the plants-side counterpart; where insects splits guilds from a {@code beneficial} flag,
 * plants carries a single {@link PlantRole} set, because {@code PlantRole} already mixes
 * ecological function ({@code NITROGEN_FIXER}, {@code KEYSTONE_HOST}) with horticultural
 * purpose ({@code FOOD_CROP}, {@code ORNAMENTAL}) and the garden reads them together.
 * <p>
 * One record per taxon — uniqueness is on {@code plantName}. A taxon whose ecology has not
 * been characterised has <em>no</em> record, never a record with an empty set: an empty
 * role set is an invariant violation, because a role record with no role has nothing to say.
 */
public record PlantEcologicalRole(
        PlantEcologicalRoleId id,
        @JsonTypeInfo(use = Id.NAME, property = "plantRank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = PlantOrderName.class, name = "ORDER"),
                @Type(value = PlantFamilyName.class, name = "FAMILY"),
                @Type(value = PlantGenusName.class, name = "GENUS"),
                @Type(value = PlantSpeciesName.class, name = "SPECIES")
        })
        PlantRankName plantName,
        Set<PlantRole> roles
) implements Entity<PlantEcologicalRoleId> {

    /**
     * Whether this taxon is a confirmed keystone host — an obligate larval food plant for
     * a keystone insect species at Oak Vista. Keystone hosts carry zero-pesticide
     * constraints, surfaced through the taxon's {@link com.naturalist.plants.management.PlantProgram}s.
     */
    public boolean isKeystoneHost() {
        return roles.contains(PlantRole.KEYSTONE_HOST);
    }

    /**
     * Whether this taxon supports parasitoid insects — tachinid flies, braconid wasps —
     * through its flower structure or nectar chemistry. These are the critical
     * infrastructure of biological pest control.
     */
    public boolean supportsBiocontrolInsects() {
        return roles.contains(PlantRole.BENEFICIAL_INSECT_HABITAT);
    }

    /**
     * Whether this taxon contributes to soil nitrogen cycling through biological fixation,
     * reducing fertiliser requirements in adjacent zones.
     */
    public boolean isNitrogenFixer() {
        return roles.contains(PlantRole.NITROGEN_FIXER);
    }

    /** Generic membership check, for callers with a role that has no named predicate. */
    public boolean playsRole(PlantRole role) {
        return roles.contains(role);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(plantName, "plantName")
                .notEmpty(roles, "roles");
    }
}
