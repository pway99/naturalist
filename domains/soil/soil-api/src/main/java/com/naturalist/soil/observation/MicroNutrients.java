package com.naturalist.soil.observation;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The micronutrient readings assembled for one analysis — the FGL "Micro Nutrients" section: zinc,
 * manganese, iron, copper, boron, and chloride. A {@link ReadModel} (it composes
 * {@link NutrientReading} entities).
 * <p>
 * Every slot is {@link Optional}; an empty slot means the lab did not report that nutrient, which
 * is not the same as reporting zero. See {@link NutrientPanel#slot}. Micros are the likeliest
 * section to come back short — labs vary most in which trace elements they run.
 */
public record MicroNutrients(
        Optional<NutrientReading> zinc,
        Optional<NutrientReading> manganese,
        Optional<NutrientReading> iron,
        Optional<NutrientReading> copper,
        Optional<NutrientReading> boron,
        Optional<NutrientReading> chloride
) implements ReadModel {

    public MicroNutrients {
        zinc = NutrientPanel.slot(zinc);
        manganese = NutrientPanel.slot(manganese);
        iron = NutrientPanel.slot(iron);
        copper = NutrientPanel.slot(copper);
        boron = NutrientPanel.slot(boron);
        chloride = NutrientPanel.slot(chloride);
    }

    /** The readings this section actually carries, in printed order. Absent slots are skipped. */
    public Stream<NutrientReading> present() {
        return Stream.of(zinc, manganese, iron, copper, boron, chloride).flatMap(Optional::stream);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntityOrNull(zinc.orElse(null), "zinc")
                .namedEntityOrNull(manganese.orElse(null), "manganese")
                .namedEntityOrNull(iron.orElse(null), "iron")
                .namedEntityOrNull(copper.orElse(null), "copper")
                .namedEntityOrNull(boron.orElse(null), "boron")
                .namedEntityOrNull(chloride.orElse(null), "chloride");
    }
}
