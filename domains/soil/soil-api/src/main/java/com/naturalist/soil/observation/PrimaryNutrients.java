package com.naturalist.soil.observation;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The primary (macro) nutrient readings assembled for one analysis — the FGL "Primary Nutrients"
 * section: nitrate-nitrogen, phosphorus (as P₂O₅), and potassium (as K₂O) in both its exchangeable
 * and soluble fractions. A {@link ReadModel} (it composes {@link NutrientReading} entities).
 * <p>
 * Every slot is {@link Optional}. The section names the FGL <em>tomato</em> panel's rows, and a
 * panel run for another crop need not report all of them — an empty slot means the lab did not
 * report that nutrient. <b>Absent is not zero</b>, and consumers must render the two differently.
 * A null slot is normalised to {@link Optional#empty()} at construction.
 */
public record PrimaryNutrients(
        Optional<NutrientReading> nitrateN,
        Optional<NutrientReading> phosphorusP2O5,
        Optional<NutrientReading> potassiumExch,
        Optional<NutrientReading> potassiumSoluble
) implements ReadModel {

    public PrimaryNutrients {
        nitrateN = NutrientPanel.slot(nitrateN);
        phosphorusP2O5 = NutrientPanel.slot(phosphorusP2O5);
        potassiumExch = NutrientPanel.slot(potassiumExch);
        potassiumSoluble = NutrientPanel.slot(potassiumSoluble);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntityOrNull(nitrateN.orElse(null), "nitrateN")
                .namedEntityOrNull(phosphorusP2O5.orElse(null), "phosphorusP2O5")
                .namedEntityOrNull(potassiumExch.orElse(null), "potassiumExch")
                .namedEntityOrNull(potassiumSoluble.orElse(null), "potassiumSoluble");
    }
}
