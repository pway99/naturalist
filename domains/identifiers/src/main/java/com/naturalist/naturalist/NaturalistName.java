package com.naturalist.naturalist;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Strongly typed natural key for {@code Naturalist} entities.
 * <p>
 * The slug is a stable, URL-safe identifier for a person in the system.
 * Example: {@code NaturalistName.of("patrick-way")}, {@code NaturalistName.of("young-delia")}.
 * <p>
 * The slug is the primary reference used in JSON catalogs and in cross-domain
 * soft references. It is never null in catalog data.
 */
public final class NaturalistName extends EntityName {

    private NaturalistName(String value) {
        super(value);
    }

    @JsonCreator
    public static NaturalistName of(String value) {
        return new NaturalistName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
