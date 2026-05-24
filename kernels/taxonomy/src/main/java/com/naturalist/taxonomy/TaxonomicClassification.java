package com.naturalist.taxonomy;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The Linnaean taxonomic placement of a catalogued organism, from order down to species.
 * <p>
 * Oak Vista catalog identifications range from species-level precision
 * (e.g. <i>Hippodamia convergens</i>, <i>Vanessa cardui</i>) to family-level
 * (e.g. Tachinidae, Carabidae) where field identification cannot distinguish species.
 * The {@code genus} and {@code species} fields are nullable to accommodate family-level
 * and genus-level identifications without sacrificing type safety.
 * <p>
 * <b>Naming conventions:</b> all fields use the standard Linnaean convention —
 * capitalised order, family, and genus; lowercase species epithet. Each rank type
 * enforces its own capitalisation rule via {@code isValid()}.
 * The binomial name is derived via {@link #binomialName()}.
 */
public record TaxonomicClassification(
        TaxonomicOrder order,
        TaxonomicFamily family,
        @Nullable TaxonomicGenus genus,
        @Nullable TaxonomicSpecies species
) implements ValueObject {

    /**
     * Returns the full binomial name if both genus and species are known,
     * the genus followed by "sp." if only genus is known, or the family
     * followed by "sp." if only family is known.
     *
     * @return the most precise scientific name available for display
     */
    public String binomialName() {
        if (genus != null && species != null) return genus.value() + " " + species.value();
        if (genus != null) return genus.value() + " sp.";
        return family.value() + " sp.";
    }

    /**
     * Whether this identification was resolved to species level.
     *
     * @return {@code true} if both genus and species are present
     */
    public boolean isSpeciesLevel() {
        return genus != null && species != null;
    }

    /**
     * Whether this classification's {@link #genus} is the given epithet.
     * Returns {@code false} when this classification has no genus assigned
     * (family-level identification) or when {@code other} is {@code null},
     * so the predicate is safe to apply across a mixed-rank catalog without
     * the caller carrying its own null guard.
     */
    public boolean belongsToGenus(TaxonomicGenus other) {
        return genus != null && genus.equals(other);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedValue(order, "order")
                .namedValue(family, "family");
    }
}
