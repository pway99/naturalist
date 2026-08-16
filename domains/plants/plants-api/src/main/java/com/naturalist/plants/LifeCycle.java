package com.naturalist.plants;

/**
 * The life-cycle duration of a {@link PlantSpecies} — <em>how many growing seasons it
 * takes to complete its cycle</em>, the axis {@link GrowthHabit} does not capture. A vine
 * or a forb can be any of these; duration and habit are independent.
 * <p>
 * Duration governs management cadence: annuals are replanted or allowed to self-seed each
 * season; biennials flower and set seed in their second year; perennials persist and are
 * pruned or divided rather than replanted.
 */
public enum LifeCycle {

    /** Germinates, flowers, sets seed, and dies within a single growing season. */
    ANNUAL,

    /**
     * Completes its cycle over two seasons — vegetative growth the first year, flowering
     * and seed set the second. Parsley is the catalog's example.
     */
    BIENNIAL,

    /** Lives three or more seasons, persisting or dying back and regrowing. */
    PERENNIAL
}
