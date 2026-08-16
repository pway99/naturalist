package com.naturalist.plants;

/**
 * The structural growth habit of a {@link PlantSpecies} — <em>what the plant looks like
 * and how it holds itself up</em>, one of the two axes the old {@code PlantLifeForm}
 * conflated (the other is {@link LifeCycle}).
 * <p>
 * The vocabulary is the USDA PLANTS <em>Growth Habit</em> set — the coarse, familiar
 * classification a botanist or horticulturist recognises at a glance. It is deliberately
 * <em>not</em> a use category: "fruit tree" is a tree ({@link #TREE}) that happens to be
 * grown for fruit; the fruit is an agronomic concern, not a growth habit.
 */
public enum GrowthHabit {

    /** A woody plant with a single dominant trunk, over ~3 metres. Peach, fig, citrus, persimmon. */
    TREE,

    /** A woody, multi-stemmed plant without a dominant trunk, typically under ~5 metres. */
    SHRUB,

    /** A low woody plant, woody only at the base — between a shrub and a herb. */
    SUBSHRUB,

    /**
     * A non-woody, broad-leaved plant — the botanical "herb" in the structural sense
     * (not the culinary one). Tomato, borage, dill, clover, radish, basil, parsley.
     */
    FORB_HERB,

    /** A grass or grass-like plant (grasses, sedges, rushes). Tall fescue. */
    GRAMINOID,

    /**
     * A climbing or scrambling plant that relies on a support to grow vertically.
     * Habit only — a vine may be annual or perennial; that is {@link LifeCycle}.
     */
    VINE
}
