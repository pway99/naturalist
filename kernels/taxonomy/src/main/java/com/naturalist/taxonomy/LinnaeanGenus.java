package com.naturalist.taxonomy;

import com.naturalist.ddd.EntityName;

/**
 * Contract for catalog entities that represent a genus-rank Linnaean taxon.
 * <p>
 * The contract exposes the upward typed reference to the parent family
 * aggregate and the genus epithet, which is non-null at the implementing
 * entity level. The genus slug is derived mechanically from the epithet.
 *
 * <p>Implemented by per-domain genus aggregates (e.g.
 * {@code com.naturalist.insects.InsectGenus}). The implementing entity
 * supplies its own typed name via its {@code NamedEntity<GENUS_NAME>}
 * binding; this interface is concerned only with the rank-level contract.
 *
 * @param <FAMILY_NAME> the parent family aggregate's typed name
 */
public interface LinnaeanGenus<FAMILY_NAME extends EntityName> {

    FAMILY_NAME familyName();

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
