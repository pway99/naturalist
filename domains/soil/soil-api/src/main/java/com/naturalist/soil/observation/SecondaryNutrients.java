package com.naturalist.soil.observation;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * The secondary nutrient readings assembled for one analysis — the FGL "Secondary Nutrients"
 * section: calcium, magnesium, and sodium in both exchangeable and soluble fractions, plus sulfate.
 * A {@link ReadModel} (it composes {@link NutrientReading} entities).
 */
public record SecondaryNutrients(
        NutrientReading calciumExch,
        NutrientReading calciumSoluble,
        NutrientReading magnesiumExch,
        NutrientReading magnesiumSoluble,
        NutrientReading sodiumExch,
        NutrientReading sodiumSoluble,
        NutrientReading sulfate
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(calciumExch, "calciumExch")
                .namedEntity(calciumSoluble, "calciumSoluble")
                .namedEntity(magnesiumExch, "magnesiumExch")
                .namedEntity(magnesiumSoluble, "magnesiumSoluble")
                .namedEntity(sodiumExch, "sodiumExch")
                .namedEntity(sodiumSoluble, "sodiumSoluble")
                .namedEntity(sulfate, "sulfate");
    }
}
