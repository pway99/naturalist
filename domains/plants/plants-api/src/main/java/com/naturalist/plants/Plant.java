package com.naturalist.plants;

import com.naturalist.biogeography.Bioregion;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.LinnaeanSpecies;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicSpecies;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A plant species established or cultivated at Oak Vista.
 * <p>
 * The Oak Vista plant catalog covers three categories: permanent structural
 * plantings (California Pipevine, scented geranium hedge, fruit trees);
 * cover crop and habitat species (clover carpet, alyssum, dill, borage);
 * and food crops (tomatoes, Passiflora edulis). Each plant carries the full
 * Durrell four-level {@link Description}, making the catalog a living educational
 * resource as well as a management reference.
 * <p>
 * {@link PlantRole} captures all ecological and horticultural functions a plant
 * fills at Oak Vista simultaneously. {@link PlantLifeForm} governs management
 * cadence — annual replanting vs. perennial maintenance vs. tree pruning schedules.
 * <p>
 * {@code nativeBioregions} records the {@link Bioregion}s where this species
 * is native. The set is informational at the species level and informs management
 * decisions: plants native to a bioregion the application is operating in are
 * generally exempt from invasive plant concerns and have evolved alongside the
 * local pollinator and butterfly communities. California Pipevine is the
 * clearest example — Pipevine Swallowtail co-evolved with Aristolochia
 * californica specifically. An empty set means we do not assert this species
 * is native to any of the recognised bioregions; it is not a claim of unknown.
 * <p>
 * Operational management — non-negotiable constraints, schedules, observation
 * cadence — lives on {@link com.naturalist.plants.management.PlantProgram}
 * records keyed off this plant's {@link PlantName}. A plant may carry many
 * programs (e.g. pesticide-exclusion plus larval-monitoring); the botanical
 * record is intentionally free of operational fields.
 * <p>
 * {@code commonNames} carries vernacular labels (English first, other locales
 * as authored) under which a young naturalist might find this plant. The catalog
 * search index harvests these as additional surface forms; an empty set means
 * <em>no asserted common name yet</em>, not <em>none exist</em>.
 */
public record Plant(
        PlantName name,
        TaxonomicClassification taxonomy,
        Description description,
        Set<PlantRole> roles,
        PlantLifeForm lifeForm,
        Set<Bioregion> nativeBioregions,
        Set<CommonName> commonNames
) implements NamedEntity<PlantName>, LinnaeanSpecies {

    @Override
    public TaxonomicGenus genus() {
        return taxonomy.genus();
    }

    @Override
    public TaxonomicSpecies species() {
        return taxonomy.species();
    }

    /**
     * Whether this plant is a confirmed keystone host — an obligate larval
     * food plant for a keystone insect species at Oak Vista. Keystone hosts
     * trigger zero-pesticide constraints in the PestManagement module.
     */
    public boolean isKeystoneHost() {
        return roles.contains(PlantRole.KEYSTONE_HOST);
    }

    /**
     * Whether this plant specifically supports parasitoid insects (tachinid flies,
     * braconid wasps) through its flower structure or nectar chemistry.
     * These plants are critical infrastructure for biological pest control.
     */
    public boolean supportsBiocontrolInsects() {
        return roles.contains(PlantRole.BENEFICIAL_INSECT_HABITAT);
    }

    /**
     * Whether this plant contributes to soil nitrogen cycling through
     * biological fixation — reducing fertiliser requirements in adjacent zones.
     */
    public boolean isNitrogenFixer() {
        return roles.contains(PlantRole.NITROGEN_FIXER);
    }

    /**
     * Whether this species is recorded as native to the given bioregion.
     */
    public boolean isNativeTo(Bioregion bioregion) {
        return nativeBioregions.contains(bioregion);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .valueObject(taxonomy, "taxonomy")
                .valueObject(description, "description")
                .notNull(roles, "roles")
                .notNull(lifeForm, "lifeForm")
                .notNull(nativeBioregions, "nativeBioregions")
                .notNull(commonNames, "commonNames");
    }
}
