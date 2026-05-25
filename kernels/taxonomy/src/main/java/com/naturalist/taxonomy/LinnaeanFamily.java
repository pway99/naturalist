package com.naturalist.taxonomy;

import com.naturalist.ddd.EntityName;

/**
 * Contract for catalog entities that represent a family-rank Linnaean taxon.
 * <p>
 * The contract exposes the upward typed reference to the parent order
 * and the family epithet, which is non-null at the implementing entity
 * level. The family slug is derived mechanically from the epithet.
 *
 * <p>Implemented by per-domain family entities (e.g.
 * {@code com.naturalist.insects.InsectFamily}). The implementing entity
 * supplies its own typed name via its {@code NamedEntity<FAMILY_NAME>}
 * binding; this interface is concerned only with the rank-level contract.
 *
 * @param <ORDER_NAME> the parent order entity's typed name
 */
public interface LinnaeanFamily<ORDER_NAME extends EntityName> {

    ORDER_NAME orderName();

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
