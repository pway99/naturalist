package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Strongly typed natural key for {@code InsectFamily} entities.
 * <p>
 * The slug is the lowercase kebab form of the Linnaean family epithet —
 * {@code "tachinidae"}, {@code "syrphidae"}, {@code "carabidae"}.
 * Cross-domain references (e.g. an {@code InsectGenus}'s upward parent
 * reference) carry {@code InsectFamilyName} rather than importing
 * {@code insects-api}, preserving DAG integrity.
 */
public final class InsectFamilyName extends EntityName implements InsectRankName {

    private InsectFamilyName(String value) {
        super(value);
    }

    @JsonCreator
    public static InsectFamilyName of(String value) {
        return new InsectFamilyName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
