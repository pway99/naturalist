package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * The natural-key name of a soil nutrient (e.g. {@code "calcium-soluble"}, {@code "boron"}). A
 * slug rather than an enum so the reading grain is not locked to a finite nutrient set — new
 * nutrients, or another lab's panel, are new slugs. The canonical set FGL reports and their
 * agronomic categories live in {@code Nutrients} (soil-api).
 */
public final class NutrientName extends EntityName {

    private NutrientName(String value) {
        super(value);
    }

    @JsonCreator
    public static NutrientName of(String value) {
        return new NutrientName(value);
    }

    @Override
    protected int maxLength() {
        return 48;
    }
}
