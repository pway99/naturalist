package com.naturalist.taxonomy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NamedValue;

/**
 * The Linnaean family rank of a taxonomic classification (e.g. {@code Carabidae},
 * {@code Tachinidae}, {@code Apidae}).
 * <p>
 * By Linnaean convention, family names are capitalised. {@link #isValid()} enforces
 * that the value is non-null, non-blank, and begins with an uppercase letter.
 */
public record TaxonomicFamily(String value) implements NamedValue<String> {

    @JsonCreator
    public static TaxonomicFamily of(String value) {
        return new TaxonomicFamily(value);
    }

    @Override
    public boolean isValid() {
        return value != null && !value.isBlank() && Character.isUpperCase(value.charAt(0));
    }
}
