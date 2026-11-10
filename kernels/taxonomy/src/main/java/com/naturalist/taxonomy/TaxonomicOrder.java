package com.naturalist.taxonomy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NamedValue;
import org.apache.commons.lang3.StringUtils;

/**
 * The Linnaean order rank of a taxonomic classification (e.g. {@code Coleoptera},
 * {@code Lepidoptera}, {@code Hymenoptera}).
 * <p>
 * By Linnaean convention, order names are capitalised. {@link #isValid()} enforces
 * that the value is non-null, non-blank, and begins with an uppercase letter.
 */
public record TaxonomicOrder(String value) implements NamedValue<String> {

    @JsonCreator
    public static TaxonomicOrder of(String value) {
        return new TaxonomicOrder(value);
    }

    @Override
    public boolean isValid() {
        return StringUtils.isNotBlank(value) && Character.isUpperCase(value.charAt(0));
    }
}
