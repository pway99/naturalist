package com.naturalist.plants.management;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Strongly typed natural key for {@link com.naturalist.plants.management.PlantProgram}
 * — a named plant management program.
 * <p>
 * The slug describes the program itself, not the plant it governs — e.g.
 * {@code "pipevine-pesticide-exclusion"}, {@code "ber-prevention"},
 * {@code "peach-leaf-curl-dormant-copper"}. A single plant may carry many
 * programs; the foreign-key reference to the underlying plant is carried by
 * the {@code plantName} component on {@code PlantProgram} itself.
 */
public final class PlantProgramName extends EntityName {

    private PlantProgramName(String value) {
        super(value);
    }

    @JsonCreator
    public static PlantProgramName of(String value) {
        return new PlantProgramName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
