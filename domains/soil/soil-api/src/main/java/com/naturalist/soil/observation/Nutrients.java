package com.naturalist.soil.observation;

import java.util.List;
import java.util.Map;

/**
 * The canonical catalog of soil nutrients FGL reports, each paired with its agronomic
 * {@link NutrientCategory}. Single source of truth for which nutrients exist and how the panel
 * factory buckets readings into the primary/secondary/micro groups.
 * <p>
 * The {@link NutrientName} constants here are the well-known names; because {@code NutrientName}
 * is a slug, a lab that reports a nutrient not listed here still round-trips as a reading — it
 * simply has no known category until added ({@link #isKnown(NutrientName)} is false).
 */
public final class Nutrients {

    private Nutrients() {
    }

    // ── Primary ────────────────────────────────────────────────────────────────
    public static final NutrientName NITRATE_N = NutrientName.of("nitrate-n");
    public static final NutrientName PHOSPHORUS_P2O5 = NutrientName.of("phosphorus-p2o5");
    public static final NutrientName POTASSIUM_EXCHANGEABLE = NutrientName.of("potassium-exchangeable");
    public static final NutrientName POTASSIUM_SOLUBLE = NutrientName.of("potassium-soluble");

    // ── Secondary ──────────────────────────────────────────────────────────────
    public static final NutrientName CALCIUM_EXCHANGEABLE = NutrientName.of("calcium-exchangeable");
    public static final NutrientName CALCIUM_SOLUBLE = NutrientName.of("calcium-soluble");
    public static final NutrientName MAGNESIUM_EXCHANGEABLE = NutrientName.of("magnesium-exchangeable");
    public static final NutrientName MAGNESIUM_SOLUBLE = NutrientName.of("magnesium-soluble");
    public static final NutrientName SODIUM_EXCHANGEABLE = NutrientName.of("sodium-exchangeable");
    public static final NutrientName SODIUM_SOLUBLE = NutrientName.of("sodium-soluble");
    public static final NutrientName SULFATE = NutrientName.of("sulfate");

    // ── Micro ──────────────────────────────────────────────────────────────────
    public static final NutrientName ZINC = NutrientName.of("zinc");
    public static final NutrientName MANGANESE = NutrientName.of("manganese");
    public static final NutrientName IRON = NutrientName.of("iron");
    public static final NutrientName COPPER = NutrientName.of("copper");
    public static final NutrientName BORON = NutrientName.of("boron");
    public static final NutrientName CHLORIDE = NutrientName.of("chloride");

    private static final Map<NutrientName, NutrientCategory> CATEGORIES = Map.ofEntries(
            Map.entry(NITRATE_N, NutrientCategory.PRIMARY),
            Map.entry(PHOSPHORUS_P2O5, NutrientCategory.PRIMARY),
            Map.entry(POTASSIUM_EXCHANGEABLE, NutrientCategory.PRIMARY),
            Map.entry(POTASSIUM_SOLUBLE, NutrientCategory.PRIMARY),
            Map.entry(CALCIUM_EXCHANGEABLE, NutrientCategory.SECONDARY),
            Map.entry(CALCIUM_SOLUBLE, NutrientCategory.SECONDARY),
            Map.entry(MAGNESIUM_EXCHANGEABLE, NutrientCategory.SECONDARY),
            Map.entry(MAGNESIUM_SOLUBLE, NutrientCategory.SECONDARY),
            Map.entry(SODIUM_EXCHANGEABLE, NutrientCategory.SECONDARY),
            Map.entry(SODIUM_SOLUBLE, NutrientCategory.SECONDARY),
            Map.entry(SULFATE, NutrientCategory.SECONDARY),
            Map.entry(ZINC, NutrientCategory.MICRO),
            Map.entry(MANGANESE, NutrientCategory.MICRO),
            Map.entry(IRON, NutrientCategory.MICRO),
            Map.entry(COPPER, NutrientCategory.MICRO),
            Map.entry(BORON, NutrientCategory.MICRO),
            Map.entry(CHLORIDE, NutrientCategory.MICRO));

    /**
     * The row label FGL prints for each nutrient. Kept here rather than in a template for the same
     * reason as {@code OptimumRange.printedForm()}: it is what the page says, and every view of a
     * report should say it identically. Note the exchangeable / soluble split, which exists in the
     * readings and in no recommendation.
     */
    private static final Map<NutrientName, String> PRINTED_NAMES = Map.ofEntries(
            Map.entry(NITRATE_N, "Nitrate-Nitrogen"),
            Map.entry(PHOSPHORUS_P2O5, "Phosphorus-P₂O₅"),
            Map.entry(POTASSIUM_EXCHANGEABLE, "Potassium-K₂O (Exch)"),
            Map.entry(POTASSIUM_SOLUBLE, "Potassium-K₂O (Sol)"),
            Map.entry(CALCIUM_EXCHANGEABLE, "Calcium (Exch)"),
            Map.entry(CALCIUM_SOLUBLE, "Calcium (Sol)"),
            Map.entry(MAGNESIUM_EXCHANGEABLE, "Magnesium (Exch)"),
            Map.entry(MAGNESIUM_SOLUBLE, "Magnesium (Sol)"),
            Map.entry(SODIUM_EXCHANGEABLE, "Sodium (Exch)"),
            Map.entry(SODIUM_SOLUBLE, "Sodium (Sol)"),
            Map.entry(SULFATE, "Sulfate"),
            Map.entry(ZINC, "Zinc"),
            Map.entry(MANGANESE, "Manganese"),
            Map.entry(IRON, "Iron"),
            Map.entry(COPPER, "Copper"),
            Map.entry(BORON, "Boron"),
            Map.entry(CHLORIDE, "Chloride"));

    /**
     * The label FGL prints for this nutrient, or the slug itself for a nutrient not in the
     * catalog — an unknown nutrient still has to render as something.
     */
    public static String printedNameOf(NutrientName name) {
        return PRINTED_NAMES.getOrDefault(name, name.value());
    }

    /** Every catalogued nutrient, in the order FGL prints them. */
    public static final List<NutrientName> ALL = List.of(
            NITRATE_N, PHOSPHORUS_P2O5, POTASSIUM_EXCHANGEABLE, POTASSIUM_SOLUBLE,
            CALCIUM_EXCHANGEABLE, CALCIUM_SOLUBLE, MAGNESIUM_EXCHANGEABLE, MAGNESIUM_SOLUBLE,
            SODIUM_EXCHANGEABLE, SODIUM_SOLUBLE, SULFATE,
            ZINC, MANGANESE, IRON, COPPER, BORON, CHLORIDE);

    /** The catalogued nutrients of one category, in printed order. */
    public static List<NutrientName> of(NutrientCategory category) {
        return ALL.stream().filter(name -> categoryOf(name) == category).toList();
    }

    /** The agronomic category of a known nutrient. */
    public static NutrientCategory categoryOf(NutrientName name) {
        NutrientCategory category = CATEGORIES.get(name);
        if (category == null) {
            throw new IllegalArgumentException("Unknown nutrient: " + name);
        }
        return category;
    }

    /** Whether this nutrient is in the known catalog (and therefore has a category). */
    public static boolean isKnown(NutrientName name) {
        return CATEGORIES.containsKey(name);
    }
}
