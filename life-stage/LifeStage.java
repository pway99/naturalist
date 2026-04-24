package com.naturalist.insects.lifestages;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import org.jspecify.annotations.Nullable;

/**
 * A stage in the life cycle of an insect species — egg, larva, pupa, or adult —
 * modelled as a stage-qualified entity with its own identity, phenology, habitat
 * requirements, and role in the species's chemistry story (where present).
 * <p>
 * Each stage is identified by a {@link LifeStageName} composing the species slug
 * with the stage kind. The larva stage of <i>Battus philenor</i> is
 * {@code battus-philenor-larva} — a stable slug referenceable from other domains
 * and from the ecological DAG layer without committing to any specific persistence
 * representation.
 * <p>
 * The sealed hierarchy permits exactly four subtypes, matching the four stages of
 * complete insect metamorphosis. Hemimetabolous orders (Orthoptera, Blattodea,
 * Hemiptera) use only {@link EggStage} and {@link AdultStage}; their nymphal stages
 * are not modelled as distinct entities here (a future refactor may introduce a
 * {@code NymphStage} subtype if warranted).
 * <p>
 * Shared contract across all stages:
 * <ul>
 *   <li>{@link #kind()} — self-identification into the {@link LifeStageKind} vocabulary.
 *       Redundant with the concrete subtype but useful for pattern-free dispatch and
 *       for serialization round-trips.</li>
 *   <li>{@link #phenology()} — the stage's seasonal activity pattern at Oak Vista.
 *       Required; every documented stage has phenology even if coarse.</li>
 *   <li>{@link #habitat()} — where the stage lives, from the insect's perspective.
 *       Required; habitat is never absent, only partially characterised.</li>
 *   <li>{@link #chemistryRole()} — the stage's role in the species's chemical defense
 *       system, if any. Nullable: undefended species carry {@code null} on every stage;
 *       defended species carry a role per defended stage. Consistency across stages
 *       is an invariant enforced at the {@code InsectSpecies} aggregate root.</li>
 *   <li>{@link #description()} — the narrative field record for this stage. Uses the
 *       {@code field-notes} kernel's four-level {@link Description} to capture the
 *       same ecological truth at preschool, elementary, secondary, and university
 *       resolution.</li>
 * </ul>
 * <p>
 * Stage-specific state lives on the concrete subtypes: host plants and prey targets
 * on {@link LarvaStage}, diapause regulation on {@link PupaStage}, flight period and
 * nectar sources on {@link AdultStage}, egg-specific morphology on {@link EggStage}.
 * <p>
 * Invariants that span stages — the metabolous-type consistency, the chemistry-story
 * coherence — are enforced on {@code InsectSpecies} because they require visibility
 * into multiple stages simultaneously. Stage-level invariants live on each concrete
 * subtype's {@code invariants()}.
 */
public sealed interface LifeStage extends NamedEntity<LifeStageName>
        permits EggStage, LarvaStage, PupaStage, AdultStage {

    /**
     * The stage kind as a discrete vocabulary value. Redundant with the concrete
     * subtype but useful for pattern-free dispatch (e.g. constructing a
     * {@link LifeStageName} from components) and for serialization round-trips.
     */
    LifeStageKind kind();

    /**
     * The stage's seasonal activity pattern at Oak Vista. Required.
     */
    StagePhenology phenology();

    /**
     * Where this stage lives at Oak Vista, from the insect's perspective.
     * Required — habitat is never wholly absent, only partially characterised.
     */
    StageHabitat habitat();

    /**
     * The stage's role in the species's chemistry story, if any.
     * <p>
     * Nullable by design. Species without chemical defenses carry {@code null} on
     * every stage. Defended species carry a role per stage that participates in the
     * defense — acquisition at the larval stage, retention through pupation,
     * expression at the adult stage, maternal transfer to the egg. Not every stage
     * of a defended species necessarily participates; the role is present when the
     * stage plays a part in the defense system and absent otherwise.
     * <p>
     * Cross-stage consistency (roles form a coherent chain, an expressing adult has
     * an upstream acquisition source) is enforced at the {@code InsectSpecies}
     * aggregate root, not here.
     */
    @Nullable StageChemistryRole chemistryRole();

    /**
     * The narrative field record for this stage — four-resolution description per
     * the {@code field-notes} kernel.
     */
    Description description();
}
