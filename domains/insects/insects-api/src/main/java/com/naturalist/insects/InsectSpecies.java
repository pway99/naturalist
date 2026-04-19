package com.naturalist.insects;

import com.naturalist.fieldnotes.Description;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.ddd.AggregateRoot;
import com.naturalist.ddd.CatalogEntity;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A catalogued insect species (class Insecta) observed or documented at Oak Vista.
 * <p>
 * Each species carries a {@link TaxonomicClassification} from order to species and
 * a four-level {@link Description} embodying Durrell's principle — the same ecological
 * truth rendered at preschool, elementary, secondary, and university resolution.
 * <p>
 * The {@code guilds} set captures all functional ecological roles the species fills
 * at Oak Vista. A hoverfly, for example, holds both {@link FunctionalGuild#PREDATOR}
 * (larva) and {@link FunctionalGuild#POLLINATOR} (adult). Guild assignment governs
 * which application modules incorporate this species in their analyses
 * (ForagingCalendar, PestManagement).
 * <p>
 * {@code beneficial} is a pragmatic garden-management flag — true for species whose
 * net effect on tomato yield and ecosystem health is positive. All parasitoids,
 * predators, pollinators, and decomposers in the Oak Vista catalog are beneficial.
 * The field is retained for completeness: future catalog entries may include pest
 * species for educational or monitoring purposes.
 * <p>
 * {@code sightingNotes} captures field observations specific to Oak Vista —
 * dates of first observation, confirmed breeding, microhabitat, or management
 * implications (e.g. "eggs confirmed on Pipevine April 2026 — never treat Pipevine").
 * <p>
 * {@code habitatProfile} carries the structured habitat classification from
 * {@code kernels/habitat}: the zones the species occupies, its moisture and light
 * regime preferences, and the vertical layers it operates in. This is the cross-domain
 * vocabulary — the same type plants, arachnids, and other organisms will carry when
 * the catalog grows. {@code habitatRequirements} is the complementary insect-specific
 * narrative: nectar sources, shelter substrate, prey density, and phototactic behaviour
 * expressed as ecological field notes rather than structured classification.
 * <p>
 * All nullable fields — {@link #identificationFeatures}, {@link #lifeStages},
 * {@link #habitatProfile}, {@link #habitatRequirements}, {@link #gardenConnections},
 * {@link #beneficialProfile}, and {@link #ecologicalSignificance} — are populated
 * incrementally as the catalog matures. {@code beneficialProfile} is additionally
 * constrained by intent: it should only be populated when {@code beneficial} is {@code true}.
 */
@AggregateRoot
public record InsectSpecies(
        InsectSpeciesId id,
        InsectSpeciesName name,
        TaxonomicClassification taxonomy,
        Description description,
        Set<FunctionalGuild> guilds,
        boolean beneficial,
        @Nullable String sightingNotes,
        @Nullable IdentificationFeatures identificationFeatures,
        @Nullable LifeStages lifeStages,
        @Nullable HabitatProfile habitatProfile,
        @Nullable HabitatRequirements habitatRequirements,
        @Nullable GardenConnections gardenConnections,
        @Nullable BeneficialProfile beneficialProfile,
        @Nullable EcologicalSignificance ecologicalSignificance
) implements CatalogEntity<InsectSpeciesId, InsectSpeciesName> {

    @Override
    public InsectSpecies withId(InsectSpeciesId id) {
        return new InsectSpecies(id, name, taxonomy, description, guilds, beneficial, sightingNotes,
                identificationFeatures, lifeStages, habitatProfile, habitatRequirements,
                gardenConnections, beneficialProfile, ecologicalSignificance);
    }

    /**
     * Whether this species performs pollination services at Oak Vista.
     * Convenience query over {@code guilds.contains(FunctionalGuild.POLLINATOR)}.
     */
    public boolean isPollinator() {
        return guilds.contains(FunctionalGuild.POLLINATOR);
    }

    /**
     * Whether this species is a keystone species at Oak Vista.
     * Keystone species require heightened management caution — never apply
     * pesticides to host plants of keystone species.
     */
    public boolean isKeystone() {
        return guilds.contains(FunctionalGuild.KEYSTONE);
    }

    /**
     * Whether this species provides direct pest suppression via predation or parasitism.
     */
    public boolean isBiocontrolAgent() {
        return guilds.contains(FunctionalGuild.PARASITOID)
                || guilds.contains(FunctionalGuild.PREDATOR)
                || guilds.contains(FunctionalGuild.APEX_PREDATOR);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        // identificationFeatures, lifeStages, habitatProfile, habitatRequirements,
        // gardenConnections, beneficialProfile, and ecologicalSignificance are intentionally
        // nullable — catalog entries are populated incrementally as field data is documented.
        return i -> i
                .entityId(id, "id")
                .entityName(name, "name")
                .notNull(this, InsectSpecies::taxonomy, "taxonomy")
                .notNull(this, InsectSpecies::description, "description")
                .notNull(this, InsectSpecies::guilds, "guilds");
    }
}
