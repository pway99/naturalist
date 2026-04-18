package com.naturalist.soil.event;

/**
 * The unit of measure for a soil amendment application quantity.
 * <p>
 * Amendment quantities at Oak Vista are recorded as applied to a specific spatial target
 * (zone or sub-zone) — not as a rate per unit area. The application layer converts
 * to per-area rates using the target's {@code areaSqft} when needed for nutrient budget
 * calculations.
 * <p>
 * <b>Convention:</b> Granular and powder amendments (blood meal, gypsum, elemental sulfur)
 * are measured by weight. Liquid amendments (fish emulsion, kelp extract) are measured by
 * volume.
 */
public enum AmendmentUnit {

    /**
     * Pounds (weight).
     * <p>
     * Standard unit for dry granular and powder amendments: gypsum, blood meal,
     * elemental sulfur, borax, potassium sulfate.
     */
    POUNDS,

    /**
     * Grams (weight).
     * <p>
     * Used for micronutrient amendments (boron, zinc, manganese) where application
     * rates are small and pound-level precision is insufficient.
     */
    GRAMS,

    /**
     * Fluid ounces (volume).
     * <p>
     * Used for liquid concentrate amendments applied in small quantities:
     * fish emulsion, kelp extract, liquid calcium.
     */
    FLUID_OUNCES,

    /**
     * Gallons (volume).
     * <p>
     * Used for diluted liquid amendments applied by drench or irrigation injection:
     * compost tea, diluted fish emulsion.
     */
    GALLONS,

    /**
     * Tablespoons (volume).
     * <p>
     * Used for small-batch liquid applications measured in the field:
     * neem oil concentrate, insecticidal soap concentrate.
     * Example: "Neem 2 tbsp + Kirkland dish soap 2 tbsp per gallon."
     */
    TABLESPOONS
}
