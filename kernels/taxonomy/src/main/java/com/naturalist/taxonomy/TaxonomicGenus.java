package com.naturalist.taxonomy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NamedValue;
import org.apache.commons.lang3.StringUtils;

/**
 * The Linnaean genus rank of a taxonomic classification (e.g. {@code Hippodamia},
 * {@code Vanessa}, {@code Bombus}).
 * <p>
 * By Linnaean convention, genus names are capitalised. {@link #isValid()} enforces
 * that the value, when present, is non-blank and begins with an uppercase letter.
 * A null value is structurally permitted — genus is absent for family-level
 * identifications where field observation cannot distinguish further.
 *
 * @see TaxonomicClassification#isSpeciesLevel()
 */
public record TaxonomicGenus(String value) implements NamedValue<String> {

    @JsonCreator
    public static TaxonomicGenus of(String value) {
        return new TaxonomicGenus(value);
    }

    @Override
    public boolean isValid() {
        return StringUtils.isNotBlank(value) && Character.isUpperCase(value.charAt(0));
    }
}
