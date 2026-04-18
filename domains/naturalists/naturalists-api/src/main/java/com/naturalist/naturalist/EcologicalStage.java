package com.naturalist.naturalist;

/**
 * A naturalist's current stage of ecological understanding — the Durrell
 * progression applied to a person rather than to a description.
 * <p>
 * Gerald Durrell held that the same ecological truth can be expressed at four
 * distinct levels of resolution, each simultaneously valid and non-contradictory.
 * The {@link com.naturalist.fieldnotes.Description} ValueObject captures all four
 * levels for every catalog entity. {@code EcologicalStage} captures where a
 * particular {@link Naturalist} currently sits in that progression — which
 * resolution of truth is most accessible to them.
 * <p>
 * Stage is not a value judgement — the wonder of WONDER is irreplaceable and
 * not superseded by NATURALIST. It is simply a guide for how to communicate
 * with and educate each person, and which Description level to surface in the
 * application layer when presenting catalog knowledge.
 * <p>
 * Stage advances through direct observation, guided learning, and time spent
 * in the field. It is not age-gated — an adult encountering ecology for the
 * first time begins at WONDER; a child raised in a naturalist household may
 * reach PRACTITIONER before adulthood.
 */
public enum EcologicalStage {

    /**
     * Direct observation, naming, sensory experience, and wonder.
     * Corresponds to the preschool level of the Durrell Description.
     * <p>
     * The naturalist at this stage asks: <em>what is that?</em> and <em>can I touch it?</em>
     * They notice colour, movement, size, and texture. They form the foundational
     * emotional connection to the natural world that makes all subsequent learning possible.
     * <p>
     * The appropriate application response is the {@code preschool} Description field.
     */
    WONDER,

    /**
     * Simple ecological relationships — food chains, predator-prey, pollination,
     * seasonal cycles observed through repeated field visits.
     * Corresponds to the elementary level of the Durrell Description.
     * <p>
     * The naturalist at this stage asks: <em>what does it eat?</em> and <em>why is it here?</em>
     * They understand that the ladybug eats the aphid and the aphid eats the plant.
     * They begin to see the garden as a connected system.
     * <p>
     * The appropriate application response is the {@code elementary} Description field.
     */
    CURIOUS,

    /**
     * Mechanisms, life cycles, field identification, and evidence-based inference.
     * Corresponds to the secondary level of the Durrell Description.
     * <p>
     * The naturalist at this stage asks: <em>how does it work?</em> They understand
     * parasitoid biology, soil chemistry implications, nematode foraging strategy,
     * and can read field sign (aphid mummies, egg masses, frass) to diagnose
     * ecological events. They can manage the garden with biological precision.
     * <p>
     * The appropriate application response is the {@code secondary} Description field.
     */
    PRACTITIONER,

    /**
     * Precise taxonomy, quantitative ecology, systems-level thinking, and
     * the ability to communicate at all lower levels with equal authenticity.
     * Corresponds to the university level of the Durrell Description.
     * <p>
     * The naturalist at this stage holds the full technical picture and can also
     * sit on a log with a child and share genuine wonder at a ground beetle.
     * They are what Durrell himself embodied: a naturalist in the complete sense.
     * <p>
     * The appropriate application response is the {@code university} Description field.
     */
    NATURALIST
}
