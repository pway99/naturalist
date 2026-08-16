package com.naturalist.plants.cultivar;

/**
 * The seed saving policy for a {@link Cultivar}, encoding whether and how
 * seeds should be collected and preserved each season.
 * <p>
 * This policy is derived from the cultivar's {@link VarietyType} and its
 * heritage significance. An open-pollinated heirloom with irreplaceable
 * provenance demands {@link #SAVE_ANNUALLY}; a commercial F1 hybrid is
 * {@link #DO_NOT_SAVE}.
 */
public enum SeedSavingPolicy {

    /**
     * Seeds must be saved every season without exception. Reserved for
     * heritage varieties with irreplaceable genetic lineage. Nick's Italian
     * Pear carries this policy — the 50+ year selection must be preserved
     * and continued.
     */
    SAVE_ANNUALLY,

    /**
     * Do not save seed from this cultivar. F1 hybrids that will not breed
     * true, or varieties where saved seed is otherwise inappropriate.
     */
    DO_NOT_SAVE,

    /**
     * Seeds may be saved if open-pollinated status is confirmed from a
     * reliable source. Amish Paste from The PlantSpecies Barn carries this policy
     * pending seed provenance verification from Baker Creek.
     */
    CONDITIONAL,

    /**
     * Seed saving is not relevant for this cultivar — either it is a
     * short-term evaluation planting or seed saving is impractical.
     */
    NOT_APPLICABLE
}
