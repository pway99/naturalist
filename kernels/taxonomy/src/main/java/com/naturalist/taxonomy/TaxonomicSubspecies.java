package com.naturalist.taxonomy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NamedValue;

/**
 * The Linnaean subspecies epithet of a taxonomic classification (e.g.
 * {@code orientis}, {@code septentrionalis}).
 * <p>
 * By convention, subspecies epithets are lowercase. {@link #isValid()}
 * enforces non-blank and lowercase-leading. A null value is not permitted at
 * the {@link LinnaeanSubspecies} level — an entity at subspecies rank
 * without a subspecies epithet has no trinomial identity.
 */
public record TaxonomicSubspecies(String value) implements NamedValue<String> {

    @JsonCreator
    public static TaxonomicSubspecies of(String value) {
        return new TaxonomicSubspecies(value);
    }

    @Override
    public boolean isValid() {
        return value != null && !value.isBlank() && Character.isLowerCase(value.charAt(0));
    }
}
