package com.naturalist.soil.observation;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The nutrient chemistry of a single soil analysis, assembled from its {@link NutrientReading}
 * entities and grouped by the FGL taxonomy: {@link PrimaryNutrients}, {@link SecondaryNutrients},
 * {@link MicroNutrients}. A {@link ReadModel} built by {@code SoilProfileFactory}.
 * <p>
 * The panel is measured fact only — no status or BER-risk judgment. Those are interpretation
 * (they depend on the crop's optimum ranges) and belong to the CropProfile effort, applied over
 * this panel rather than stored in it. The physical/derived parameters are a separate typed entity
 * family — see {@link SoilPhysicalCharacteristics}.
 * <p>
 * <b>The three groups are a shape, not a guarantee.</b> Their slots name the rows of the FGL
 * tomato panel; every slot is {@link Optional} because another lab or another crop's panel may
 * not report all of them. A panel missing a nutrient is a valid panel — see {@link #slot}.
 */
public record NutrientPanel(
        PrimaryNutrients primary,
        SecondaryNutrients secondary,
        MicroNutrients micro
) implements ReadModel {

    /**
     * Normalises a panel slot: a null {@link Optional} becomes {@link Optional#empty()}. Both mean
     * "this lab did not report that nutrient", and collapsing them at construction keeps every
     * consumer — invariants, templates, interpretation — on a single absent representation.
     * <p>
     * Absent is <b>not</b> zero. A zero reading is a measurement the lab made; an empty slot is a
     * measurement it never made, and no interpretation may treat the two alike.
     */
    static Optional<NutrientReading> slot(@Nullable Optional<NutrientReading> reading) {
        return reading == null ? Optional.empty() : reading;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .readModel(primary, "primary")
                .readModel(secondary, "secondary")
                .readModel(micro, "micro");
    }
}
