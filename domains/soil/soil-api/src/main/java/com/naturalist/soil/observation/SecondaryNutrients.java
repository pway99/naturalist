package com.naturalist.soil.observation;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The secondary nutrient readings assembled for one analysis — the FGL "Secondary Nutrients"
 * section: calcium, magnesium, and sodium in both exchangeable and soluble fractions, plus sulfate.
 * A {@link ReadModel} (it composes {@link NutrientReading} entities).
 * <p>
 * Every slot is {@link Optional}; an empty slot means the lab did not report that nutrient, which
 * is not the same as reporting zero. See {@link NutrientPanel#slot}.
 */
public record SecondaryNutrients(
        Optional<NutrientReading> calciumExch,
        Optional<NutrientReading> calciumSoluble,
        Optional<NutrientReading> magnesiumExch,
        Optional<NutrientReading> magnesiumSoluble,
        Optional<NutrientReading> sodiumExch,
        Optional<NutrientReading> sodiumSoluble,
        Optional<NutrientReading> sulfate
) implements ReadModel {

    public SecondaryNutrients {
        calciumExch = NutrientPanel.slot(calciumExch);
        calciumSoluble = NutrientPanel.slot(calciumSoluble);
        magnesiumExch = NutrientPanel.slot(magnesiumExch);
        magnesiumSoluble = NutrientPanel.slot(magnesiumSoluble);
        sodiumExch = NutrientPanel.slot(sodiumExch);
        sodiumSoluble = NutrientPanel.slot(sodiumSoluble);
        sulfate = NutrientPanel.slot(sulfate);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntityOrNull(calciumExch.orElse(null), "calciumExch")
                .namedEntityOrNull(calciumSoluble.orElse(null), "calciumSoluble")
                .namedEntityOrNull(magnesiumExch.orElse(null), "magnesiumExch")
                .namedEntityOrNull(magnesiumSoluble.orElse(null), "magnesiumSoluble")
                .namedEntityOrNull(sodiumExch.orElse(null), "sodiumExch")
                .namedEntityOrNull(sodiumSoluble.orElse(null), "sodiumSoluble")
                .namedEntityOrNull(sulfate.orElse(null), "sulfate");
    }
}
