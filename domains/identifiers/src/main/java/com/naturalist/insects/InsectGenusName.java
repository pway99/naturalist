package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;
import com.naturalist.taxonomy.LinealRank;

/**
 * Strongly typed natural key for {@code InsectGenus} entities.
 * <p>
 * The slug is the lowercase kebab form of the Linnaean genus epithet —
 * {@code "halictus"}, {@code "chrysoperla"}, {@code "battus"}. Cross-domain
 * references (e.g. an {@code InsectSpecies}'s upward parent reference) carry
 * {@code InsectGenusName} rather than importing {@code insects-api},
 * preserving DAG integrity.
 */
public final class InsectGenusName extends EntityName implements InsectRankName {

    private InsectGenusName(String value) {
        super(value);
    }

    @JsonCreator
    public static InsectGenusName of(String value) {
        return new InsectGenusName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }

    @Override
    public LinealRank rank() {
        return LinealRank.GENUS;
    }
}
