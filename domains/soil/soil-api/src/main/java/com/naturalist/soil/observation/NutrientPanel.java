package com.naturalist.soil.observation;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * The nutrient chemistry of a single soil analysis, assembled from its {@link NutrientReading}
 * entities and grouped by the FGL taxonomy: {@link PrimaryNutrients}, {@link SecondaryNutrients},
 * {@link MicroNutrients}. A {@link ReadModel} built by {@code NutrientPanelFactory}.
 * <p>
 * The panel is measured fact only — no status or BER-risk judgment. Those are interpretation
 * (they depend on the crop's optimum ranges) and belong to the CropProfile effort, applied over
 * this panel rather than stored in it. The physical/derived parameters are a separate typed entity
 * family — see {@link SoilPhysicalCharacteristics}.
 */
public record NutrientPanel(
        PrimaryNutrients primary,
        SecondaryNutrients secondary,
        MicroNutrients micro
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .readModel(primary, "primary")
                .readModel(secondary, "secondary")
                .readModel(micro, "micro");
    }
}
