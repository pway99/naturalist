package com.naturalist.taxonomy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NamedValue;

/**
 * The Linnaean species epithet of a taxonomic classification (e.g. {@code convergens},
 * {@code cardui}, {@code terrestris}).
 * <p>
 * By Linnaean convention, species epithets are lowercase. {@link #isValid()} enforces
 * that the value, when present, is non-blank and begins with a lowercase letter.
 * A null value is structurally permitted — species is absent for family-level and
 * genus-level identifications where field observation cannot distinguish further.
 *
 * @see TaxonomicClassification#isSpeciesLevel()
 * @see TaxonomicClassification#binomialName()
 */
public record TaxonomicSpecies(String value) implements NamedValue<String> {

    @JsonCreator
    public static TaxonomicSpecies of(String value) {
        return new TaxonomicSpecies(value);
    }

    @Override
    public boolean isValid() {
        return value != null && !value.isBlank() && Character.isLowerCase(value.charAt(0));
    }
}
