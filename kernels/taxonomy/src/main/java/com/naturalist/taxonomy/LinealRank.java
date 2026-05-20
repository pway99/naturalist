package com.naturalist.taxonomy;

/**
 * The closed Linnaean ladder of taxonomic ranks, ordered from broadest to most specific.
 *
 * <p>An organism-agnostic discriminator for the rank concept itself, independent of the
 * typed name carrying the slug. Insect-side rank names ({@code InsectFamilyName},
 * {@code InsectGenusName}, {@code InsectSpeciesName}, {@code InsectSubspeciesName}) are
 * organism-specific carriers; {@code LinealRank} is the position on the ladder those
 * carriers occupy. Future organism domains (plants, arachnids, …) reuse the same enum.
 *
 * <p>The ladder is closed by biology and declared complete from {@code KINGDOM} down to
 * {@code SUBSPECIES}, even though only the four lower ranks are currently referenced by
 * typed names. Partial coverage would mean re-editing this enum each time a domain
 * reaches a higher rank.
 *
 * <p>Ordinal ordering reflects increasing specificity: {@code KINGDOM} (most general) →
 * {@code SUBSPECIES} (most specific). Rank-transition invariants (e.g. "an identification
 * update must move down the ladder, not up") can use the natural enum comparison.
 */
public enum LinealRank {
    KINGDOM,
    PHYLUM,
    CLASS,
    ORDER,
    FAMILY,
    GENUS,
    SPECIES,
    SUBSPECIES
}
