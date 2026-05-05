package com.naturalist.taxonomy;

import com.naturalist.ddd.EntityName;

/**
 * Contract for catalog entities that represent a genus-rank Linnaean taxon.
 * <p>
 * The contract narrows {@link TaxonomicClassification}'s genus epithet to
 * non-null at the implementing entity level and exposes the upward typed
 * reference to the parent family aggregate. The genus slug is derived
 * mechanically from the epithet.
 *
 * <p>The redundant {@link #family()} epithet is exposed alongside the
 * {@link #familyName()} reference so the parent-child consistency check
 * (a genus' {@code family} epithet must match its resolved parent family's
 * {@code family} epithet) can be performed at catalog-assembly time without
 * forcing the genus to resolve its parent.
 *
 * <p>Implemented by per-domain genus aggregates (e.g.
 * {@code com.naturalist.insects.InsectGenus},
 * {@code com.naturalist.plants.PlantGenus}). The implementing entity supplies
 * its own typed name via its {@code NamedEntity<GENUS_NAME>} binding.
 *
 * @param <FAMILY_NAME> the parent family aggregate's typed name
 */
public interface LinnaeanGenus<FAMILY_NAME extends EntityName> {

    FAMILY_NAME familyName();

    TaxonomicFamily family();

    TaxonomicGenus genus();

    /**
     * The genus slug derived from the genus epithet — lowercase kebab form.
     * The slug is {@code identity}; common names are findable but not
     * authoritative.
     */
    default String genusSlug() {
        return TaxonomicSlugs.genusSlug(genus());
    }
}
