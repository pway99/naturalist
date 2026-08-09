package com.naturalist.soil.observation;

/**
 * The agronomic grouping FGL uses for the nutrients on a soil report. Drives how the panel
 * factory buckets {@code NutrientReading}s into the {@code PrimaryNutrients},
 * {@code SecondaryNutrients}, and {@code MicroNutrients} read models.
 */
public enum NutrientCategory {

    /** Nitrogen, phosphorus, potassium — the macronutrients crops draw most heavily. */
    PRIMARY,

    /** Calcium, magnesium, sodium, sulfur — needed in moderate amounts. */
    SECONDARY,

    /** Zinc, manganese, iron, copper, boron, chloride — required in trace amounts. */
    MICRO
}
