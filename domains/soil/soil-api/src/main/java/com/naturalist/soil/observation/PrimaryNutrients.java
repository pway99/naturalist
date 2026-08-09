package com.naturalist.soil.observation;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * The primary (macro) nutrient readings assembled for one analysis — the FGL "Primary Nutrients"
 * section: nitrate-nitrogen, phosphorus (as P₂O₅), and potassium (as K₂O) in both its exchangeable
 * and soluble fractions. A {@link ReadModel} (it composes {@link NutrientReading} entities).
 */
public record PrimaryNutrients(
        NutrientReading nitrateN,
        NutrientReading phosphorusP2O5,
        NutrientReading potassiumExch,
        NutrientReading potassiumSoluble
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(nitrateN, "nitrateN")
                .namedEntity(phosphorusP2O5, "phosphorusP2O5")
                .namedEntity(potassiumExch, "potassiumExch")
                .namedEntity(potassiumSoluble, "potassiumSoluble");
    }
}
