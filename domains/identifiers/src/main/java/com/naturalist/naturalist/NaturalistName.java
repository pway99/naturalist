package com.naturalist.naturalist;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;
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

    /**
     * Mints a fresh opaque handle — {@code "nat-"} plus a UUIDv7 (hex, no hyphens), so it is
     * a valid kebab slug that carries no personal information. Uses the kernel generator;
     * never {@code UUID.randomUUID()}.
     */
    public static NaturalistName create() {
        return new NaturalistName("nat-" + EntityId.newUUID().toString().replace("-", ""));
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
