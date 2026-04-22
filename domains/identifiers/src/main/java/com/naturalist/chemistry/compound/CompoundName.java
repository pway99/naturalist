package com.naturalist.chemistry.compound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Natural key for {@code Compound} — a human-readable slug stable across deployments.
 * <p>
 * Values match the {@code "name"} field in {@code compounds.json}.
 * Domain modules (Soil, Insects) reference compounds by {@code CompoundName} slug —
 * never by raw string or by importing compound classes.
 * <p>
 * Example: {@code CompoundName.of("calcium-sulfate-dihydrate")}
 */
public final class CompoundName extends EntityName {

    private CompoundName(String value) {
        super(value);
    }

    @JsonCreator
    public static CompoundName of(String value) {
        return new CompoundName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}