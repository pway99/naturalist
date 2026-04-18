package com.naturalist.plants.cultivar;

/**
 * The breeding status of a {@link Cultivar}, determining whether seed saving
 * is genetically meaningful.
 * <p>
 * Breeding status governs the seed saving policy: open-pollinated varieties
 * breed true from saved seed; F1 hybrids do not segregate predictably; F2
 * generation plants from saved F1 seed show unpredictable recombination of
 * parental traits.
 */
public enum VarietyType {

    /**
     * Open-pollinated variety that breeds true from saved seed.
     * Suitable for seed saving programs and multi-generational selection.
     * Nick's Italian Pear is the exemplar — 50+ years of selection.
     */
    OPEN_POLLINATED,

    /**
     * First-generation hybrid. Vigorous and uniform but does not breed true.
     * Seeds saved from F1 plants will segregate unpredictably in the next
     * generation. Do not save seed. Sungold Cherry is a commercial F1.
     */
    HYBRID_F1,

    /**
     * Second generation grown from saved F1 hybrid seed. Performance is
     * unpredictable — offspring segregate across parental trait combinations.
     * Useful for evaluation but not for seed saving. San Marzano saved-seed
     * plants at Oak Vista 2026 are likely F2.
     */
    HYBRID_F2,

    /**
     * Breeding status not yet confirmed. Treat conservatively — do not save
     * seed until open-pollinated status is verified from a reliable source.
     * Amish Paste from The Plant Barn is flagged UNKNOWN pending provenance
     * confirmation.
     */
    UNKNOWN
}
