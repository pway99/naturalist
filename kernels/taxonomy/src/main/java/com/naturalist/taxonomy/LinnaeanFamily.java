package com.naturalist.taxonomy;

/**
 * Contract for catalog entities that represent a family-rank Linnaean taxon.
 * <p>
 * The contract narrows {@link TaxonomicClassification}'s family epithet to
 * non-null at the implementing entity level — a family-rank record without a
 * family epithet has no stable identity. The family slug is derived
 * mechanically from the epithet, so cross-domain references compose without
 * per-entry naming judgment.
 *
 * <p>Implemented by per-domain family aggregates (e.g.
 * {@code com.naturalist.insects.InsectFamily},
 * {@code com.naturalist.plants.PlantFamily}). The implementing entity supplies
 * its own typed name via its {@code NamedEntity<FAMILY_NAME>} binding; this
 * interface is concerned only with the rank-level contract.
 */
public interface LinnaeanFamily {

    TaxonomicFamily family();

    /**
     * The family slug derived from the family epithet — lowercase kebab form.
     * The slug is {@code identity}; common names are findable but not
     * authoritative.
     */
    default String familySlug() {
        return TaxonomicSlugs.familySlug(family());
    }
}
