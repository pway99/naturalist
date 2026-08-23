package com.naturalist.soil.catalog;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.soil.observation.NutrientChemistry;
import com.naturalist.soil.observation.NutrientName;
import com.naturalist.soil.observation.Nutrients;

/**
 * Resolves a nutrient row to the console URL of the substance it measures.
 *
 * <p>Three independent answers have to line up, and any of them coming up empty
 * degrades the row to plain text rather than a broken link:
 *
 * <ol>
 *   <li>soil declares the substance ({@link Nutrients#chemistryOf}) — empty for a lab
 *       slug outside the catalog;</li>
 *   <li>the {@link Catalog} says which domain owns that slug — empty until the owning
 *       domain contributes the entity;</li>
 *   <li>the composite {@link EntityRefLinker} renders it — null when no console module
 *       serves a route for that name type.</li>
 * </ol>
 *
 * <p>Resolution goes through the catalog seam rather than a direct dependency on
 * chemistry, exactly as {@code CladeRankLinks} does for clade rank eyebrows. Soil does
 * not know that chemistry is the domain on the other end, nor whether the substance is
 * an element or a compound.
 */
public final class NutrientChemistryLinks {

    private static final NutrientChemistryLinks NONE = new NutrientChemistryLinks(null, null);

    private final Catalog catalog;
    private final EntityRefLinker linker;

    private NutrientChemistryLinks(Catalog catalog, EntityRefLinker linker) {
        this.catalog = catalog;
        this.linker = linker;
    }

    /** A resolver backed by the assembled catalog and the app's composite linker. */
    public static NutrientChemistryLinks of(Catalog catalog, EntityRefLinker linker) {
        if (catalog == null || linker == null) {
            return NONE;
        }
        return new NutrientChemistryLinks(catalog, linker);
    }

    /** A resolver that links nothing — the default for templates rendered without a catalog. */
    public static NutrientChemistryLinks none() {
        return NONE;
    }

    /**
     * The console URL for the substance this nutrient measures, or {@code null} when any
     * of the three steps above has no answer.
     */
    public String linkFor(NutrientName nutrient) {
        if (catalog == null) {
            return null;
        }
        return Nutrients.chemistryOf(nutrient)
                .map(chemistry -> chemistry.substance().value())
                .flatMap(catalog::findBySlug)
                .map(linker::linkFor)
                .orElse(null);
    }

    /**
     * A phrase naming the substance and the form the lab printed — the link's title, so a
     * reader following "Phosphorus-P₂O₅" to phosphorus knows the value was an oxide
     * equivalent. Empty when the nutrient declares no chemistry.
     */
    public String titleFor(NutrientName nutrient) {
        if (catalog == null) {
            return "";
        }
        return Nutrients.chemistryOf(nutrient)
                .map(NutrientChemistryLinks::describe)
                .orElse("");
    }

    private static String describe(NutrientChemistry chemistry) {
        return chemistry.substance().value() + " — " + chemistry.reportedForm().label();
    }
}
