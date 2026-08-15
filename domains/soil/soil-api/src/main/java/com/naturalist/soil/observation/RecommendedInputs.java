package com.naturalist.soil.observation;

import java.util.List;

/**
 * The canonical catalog of inputs FGL prints recommendations for — the counterpart to
 * {@link Nutrients} on the recommendation side of the report. Single source of truth for which
 * rows the fertilisation table and the requirements block carry.
 * <p>
 * <b>Two blocks, one vocabulary.</b> The March 2026 reports print twelve fertilisation rows in
 * lbs/1000 ft² ({@link #FERTILIZATION}) and two requirement rows in tons/acre-foot
 * ({@link #REQUIREMENTS}). Lime appears in both — as {@link #LIME}, what to apply, and as
 * {@link #LIME_REQUIREMENT}, how much the soil needs — and they are separate slugs because they
 * are separate statements that can disagree. On Box 1 they do not: the requirement is 0 Tons/AF
 * and the fertilisation row is None.
 * <p>
 * As with {@code Nutrients}, this list is curated rather than exhaustive: a lab that prints a row
 * not named here still round-trips, it simply has no known block.
 */
public final class RecommendedInputs {

    private RecommendedInputs() {
    }

    // ── Fertilization Recommendations — lbs/1000 ft² ────────────────────────────
    public static final RecommendedInputName NITROGEN = RecommendedInputName.of("nitrogen");
    public static final RecommendedInputName PHOSPHORUS_P2O5 = RecommendedInputName.of("phosphorus-p2o5");
    public static final RecommendedInputName POTASSIUM_K2O = RecommendedInputName.of("potassium-k2o");
    public static final RecommendedInputName CALCIUM = RecommendedInputName.of("calcium");
    public static final RecommendedInputName MAGNESIUM = RecommendedInputName.of("magnesium");
    public static final RecommendedInputName SULFUR = RecommendedInputName.of("sulfur");
    public static final RecommendedInputName ZINC = RecommendedInputName.of("zinc");
    public static final RecommendedInputName MANGANESE = RecommendedInputName.of("manganese");
    public static final RecommendedInputName IRON = RecommendedInputName.of("iron");
    public static final RecommendedInputName COPPER = RecommendedInputName.of("copper");
    public static final RecommendedInputName BORON = RecommendedInputName.of("boron");
    public static final RecommendedInputName LIME = RecommendedInputName.of("lime");

    // ── Requirements block — tons/acre-foot ─────────────────────────────────────
    public static final RecommendedInputName LIME_REQUIREMENT = RecommendedInputName.of("lime-requirement");
    public static final RecommendedInputName GYPSUM_REQUIREMENT = RecommendedInputName.of("gypsum-requirement");

    /** The twelve rows of the Fertilization Recommendations table, in printed order. */
    public static final List<RecommendedInputName> FERTILIZATION = List.of(
            NITROGEN, PHOSPHORUS_P2O5, POTASSIUM_K2O, CALCIUM, MAGNESIUM, SULFUR,
            ZINC, MANGANESE, IRON, COPPER, BORON, LIME);

    /** The two rows of the requirements block, in printed order. */
    public static final List<RecommendedInputName> REQUIREMENTS = List.of(
            LIME_REQUIREMENT, GYPSUM_REQUIREMENT);

    /** Whether this input is in the known catalog. */
    public static boolean isKnown(RecommendedInputName name) {
        return FERTILIZATION.contains(name) || REQUIREMENTS.contains(name);
    }
}
