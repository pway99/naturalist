package com.naturalist.plants.cultivar;

/**
 * The fruit morphology and culinary use category of a tomato {@link Cultivar}.
 * <p>
 * Fruit type determines processing suitability: paste types have low water
 * content and thick flesh for sauce production; cherry types are fresh-eating
 * varieties with high sugar concentration; slicers and beefsteaks are large
 * fresh-eating varieties for sandwiches and salads.
 */
public enum FruitType {

    /**
     * Low water content, thick flesh, small seed cavity. Optimised for sauce
     * and canning. Yields thick sauce with minimal cooking time. Amish Paste,
     * Italian Pear, and San Marzano are paste types.
     */
    PASTE,

    /**
     * Small fruit, typically under 1 oz, with high sugar concentration and
     * intense flavour. Primarily for fresh eating. Sungold Cherry is the
     * exemplar — intensely sweet orange cherry.
     */
    CHERRY,

    /**
     * Medium to large fruit with balanced flesh-to-juice ratio. Standard
     * fresh-eating variety for sandwiches and salads.
     */
    SLICER,

    /**
     * Very large fruit, often exceeding 1 lb, with dense meaty flesh.
     * Minimal seed cavities relative to size.
     */
    BEEFSTEAK
}
