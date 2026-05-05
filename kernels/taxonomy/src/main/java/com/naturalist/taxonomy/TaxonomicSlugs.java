package com.naturalist.taxonomy;

import java.util.Locale;
import java.util.Objects;

/**
 * Slug-derivation helpers for living-organism Linnaean taxa.
 * <p>
 * The binomial and trinomial slugs are mechanical functions of the rank
 * epithets — there is no per-entry slug-design judgment. An entity cannot
 * disagree with itself, because the slug is derivable from its taxonomic
 * data.
 *
 * <p>Whitespace and underscores in epithets collapse to single hyphens; the
 * result is lower-cased. Inputs are expected to satisfy their own
 * {@code NamedValue.isValid()} contract before they reach this helper.
 */
final class TaxonomicSlugs {

    private TaxonomicSlugs() {
    }

    static String binomial(TaxonomicGenus genus, TaxonomicSpecies species) {
        Objects.requireNonNull(genus, "genus");
        Objects.requireNonNull(species, "species");
        return kebab(genus.value()) + "-" + kebab(species.value());
    }

    static String trinomial(TaxonomicGenus genus, TaxonomicSpecies species, TaxonomicSubspecies subspecies) {
        Objects.requireNonNull(subspecies, "subspecies");
        return binomial(genus, species) + "-" + kebab(subspecies.value());
    }

    private static String kebab(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[\\s_]+", "-");
    }
}
