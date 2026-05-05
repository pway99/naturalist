package com.naturalist.taxonomy;

/**
 * Contract for catalog entities that represent a species-rank Linnaean taxon.
 * <p>
 * The contract narrows {@link TaxonomicClassification}'s nullable genus and species
 * to non-null at the implementing entity level — a species-rank record without
 * either epithet has no stable identity. Implementing entities also expose the
 * binomial slug derived from those epithets, so cross-domain references compose
 * mechanically rather than by per-entry naming judgment.
 *
 * <p>Implemented by {@code com.naturalist.plants.Plant} and
 * {@code com.naturalist.insects.InsectSpecies}; any future living-organism
 * aggregate at species rank should implement this interface rather than
 * carrying a free-form slug.
 */
public interface LinnaeanSpecies {

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
