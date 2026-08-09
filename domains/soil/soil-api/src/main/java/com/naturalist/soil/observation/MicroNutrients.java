package com.naturalist.soil.observation;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * The micronutrient readings assembled for one analysis — the FGL "Micro Nutrients" section: zinc,
 * manganese, iron, copper, boron, and chloride. A {@link ReadModel} (it composes
 * {@link NutrientReading} entities). Each reading's {@code status} is the lab's own band, not a
 * value-versus-range recomputation (FGL often marks an above-optimum micro as satisfactory).
 */
public record MicroNutrients(
        NutrientReading zinc,
        NutrientReading manganese,
        NutrientReading iron,
        NutrientReading copper,
        NutrientReading boron,
        NutrientReading chloride
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(zinc, "zinc")
                .namedEntity(manganese, "manganese")
                .namedEntity(iron, "iron")
                .namedEntity(copper, "copper")
                .namedEntity(boron, "boron")
                .namedEntity(chloride, "chloride");
    }
}
