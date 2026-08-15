package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * The natural-key name of something a lab recommends applying (e.g. {@code "nitrogen"},
 * {@code "lime"}, {@code "gypsum-requirement"}). A slug rather than an enum, for the same reason
 * {@link NutrientName} is: another lab's recommendation table is a different set of rows.
 * <p>
 * <b>Deliberately not {@link NutrientName}.</b> The two vocabularies overlap but are not the same
 * list, and conflating them would be wrong in both directions. A recommendation of
 * {@code "nitrogen"} is not the {@code "nitrate-n"} that was measured — you apply nitrogen, you
 * measure the nitrate fraction of it. {@code "sulfur"} is not {@code "sulfate"}. {@code "lime"} is
 * not a nutrient at all. And the readings split calcium and magnesium into exchangeable and
 * soluble fractions, which no recommendation does. The canonical set FGL prints lives in
 * {@code RecommendedInputs} (soil-api).
 */
public final class RecommendedInputName extends EntityName {

    private RecommendedInputName(String value) {
        super(value);
    }

    @JsonCreator
    public static RecommendedInputName of(String value) {
        return new RecommendedInputName(value);
    }

    @Override
    protected int maxLength() {
        return 48;
    }
}
