package com.naturalist.taxonomy;

import com.naturalist.ddd.EntityName;

/**
 * Contract for catalog entities that represent a species-rank Linnaean taxon.
 * <p>
 * The contract narrows {@link TaxonomicClassification}'s nullable genus and species
 * to non-null at the implementing entity level — a species-rank record without
 * either epithet has no stable identity. Implementing entities also expose the
 * binomial slug derived from those epithets, so cross-domain references compose
 * mechanically rather than by per-entry naming judgment.
 *
 * <p>The {@link #genusName()} component is the upward typed reference to the
 * parent genus aggregate. It is non-null: every species in the catalog points at
 * its genus, and the consistency invariant (a species' {@code genus} epithet
 * must match its resolved parent genus' {@code genus} epithet) is validated at
 * catalog-assembly time.
 *
 * <p>Implemented by {@code com.naturalist.plants.Plant} and
 * {@code com.naturalist.insects.InsectSpecies}; any future living-organism
 * aggregate at species rank should implement this interface rather than
 * carrying a free-form slug.
 *
 * @param <GENUS_NAME> the parent genus aggregate's typed name
 */
public interface LinnaeanSpecies<GENUS_NAME extends EntityName> {

    GENUS_NAME genusName();

    TaxonomicGenus genus();

    TaxonomicSpecies species();

    /**
     * The binomial slug derived from genus and species epithets — lowercase
     * kebab form, single hyphen separator. The slug is {@code identity}; common
     * names are findable but not authoritative.
     */
    default String binomialSlug() {
        return TaxonomicSlugs.binomial(genus(), species());
    }
}
