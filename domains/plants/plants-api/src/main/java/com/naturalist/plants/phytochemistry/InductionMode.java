package com.naturalist.plants.phytochemistry;

/**
 * When a plant produces a secondary metabolite — the temporal axis of
 * phytochemical expression.
 * <p>
 * Induction is a first-class field because two compounds with the same
 * structural type and the same ecological role can carry very different
 * management implications depending on when they appear. Glucosinolates
 * sit constitutively in brassica leaves (constant herbivore deterrent);
 * jasmonate-induced volatile terpenes in the same leaf surface only after
 * caterpillar damage (signaling to parasitoid wasps). Anthocyanin pigments
 * in fruit are developmental — present at ripening, absent before.
 * <p>
 * A {@code PhytochemicalConstituent} declares a single induction mode; a
 * compound that operates in two modes (e.g. low constitutive plus a strong
 * induced burst) is recorded as two separate constituents with different
 * tissue/induction profiles, and that decomposition is a feature, not a
 * limitation.
 */
public enum InductionMode {

    /** Always present at a baseline concentration. */
    CONSTITUTIVE,

    /** Produced in response to herbivory, infection, wounding, drought, or other stress. */
    INDUCED,

    /** Tied to a developmental stage — flowering, ripening, senescence. */
    DEVELOPMENTAL
}
