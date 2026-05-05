package com.naturalist.taxonomy;

import com.naturalist.ddd.Named;

/**
 * Contract for catalog entities that represent a subspecies-rank Linnaean taxon.
 * <p>
 * The contract exposes the parent species reference (typed via the framework's
 * {@link Named} generic so each domain plugs in its own typed parent name)
 * alongside the full trinomial — genus, species, subspecies epithet — and
 * the derived trinomial slug.
 *
 * <p>No aggregate implements this interface today; it ships as the documented
 * contract for the moment a subspecies record first needs to enter a catalog.
 *
 * @param <PARENT> the parent species's {@code Named} type
 */
public interface LinnaeanSubspecies<PARENT extends Named<?>> {

    PARENT parentSpecies();

    TaxonomicGenus genus();

    TaxonomicSpecies species();

    TaxonomicSubspecies subspeciesEpithet();

    /**
     * The trinomial slug derived from genus, species, and subspecies epithets —
     * lowercase kebab form, single hyphen separators.
     */
    default String trinomialSlug() {
        return TaxonomicSlugs.trinomial(genus(), species(), subspeciesEpithet());
    }
}
