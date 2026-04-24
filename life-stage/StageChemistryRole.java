package com.naturalist.insects.lifestages;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The role a life stage plays in its species's chemistry story.
 * <p>
 * Chemical defense in insects is frequently a multi-stage system rather than a
 * single-stage trait. <i>Battus philenor</i> is the canonical case: the larva
 * <b>acquires</b> aristolochic acids by feeding on <i>Aristolochia californica</i>;
 * the pupa <b>retains</b> them through metamorphosis; the adult <b>expresses</b>
 * them in wing scales and hemolymph (reinforced by aposematic colouration); and the
 * female <b>transfers</b> them maternally to the brick-red egg clusters. Four stages,
 * four roles, one integrated defense system.
 * <p>
 * Modelling chemistry participation as a per-stage role (with the species carrying
 * the narrative of the defense system as a whole) expresses the biology directly:
 * the stage knows its role, the species knows the story, and consistency between
 * the two is enforced as a species-level invariant.
 * <p>
 * The species-level chemistry story — mechanism, source compounds, aposematic
 * signal — continues to live on {@code InsectSpecies.ChemicalDefense}. What was
 * {@code ChemicalDefense.protectedStages: Set<LifeStageKind>} under the previous
 * model becomes a derived projection: walk the stages, collect those with a
 * non-null {@code chemistryRole}, you have the protected set. The invariant is
 * enforceable locally because the species aggregate root holds all stages.
 * <p>
 * {@code role} names the stage's participation: acquisition, retention, expression,
 * or maternal transfer. These four cover the <i>Battus philenor</i> case and
 * generalise to every defended species I've seen in the literature. If a fifth
 * role emerges (e.g. <i>symbiont-mediated synthesis</i> with its own stage
 * expression pattern), this enum gains a value.
 * <p>
 * {@code notes} carries stage-specific detail — the mechanism of acquisition, the
 * reason retention works through pupation when many chemistries do not survive
 * metamorphosis, the specific glands or tissues involved. Nullable; the role alone
 * is often sufficient.
 * <p>
 * Future: when the {@code chemistry} domain grows beyond Elements and Compounds, a
 * {@code compounds} field of typed references will join this record, letting the
 * DAG layer emit edges like {@code Larva --sequesters--> Aristolochic-Acid-II}.
 * For now, the compound-level detail lives on the species-level
 * {@code ChemicalDefense.sourceCompounds}.
 */
public record StageChemistryRole(
        Role role,
        @Nullable String notes
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.notNull(this, StageChemistryRole::role, "role");
    }

    /**
     * The kind of participation a stage has in its species's chemistry story.
     */
    public enum Role {
        /**
         * The stage is the point at which defensive chemistry enters the organism.
         * Typically the phytophagous larva feeding on a chemically defended host.
         * <i>Battus philenor</i> larvae acquire aristolochic acids from
         * <i>Aristolochia californica</i> leaf tissue.
         */
        ACQUISITION,

        /**
         * The stage preserves inherited chemistry through a transformation that
         * often destroys less stable compounds. Typically the pupa retaining
         * larval-acquired chemistry through metamorphosis. Biologically non-trivial:
         * many chemistries do not survive pupation, so retention is a real trait
         * rather than the default.
         */
        RETENTION,

        /**
         * The stage displays or deploys the chemistry — the stage at which the
         * defense is actively presented to predators, typically the adult with
         * aposematic colouration or the chemically defended adult carrying
         * ingested compounds in hemolymph or wing scales.
         */
        EXPRESSION,

        /**
         * The stage receives chemistry from a parent rather than acquiring it
         * from the environment. Typically the egg, receiving maternal investment
         * of defensive compounds. <i>Battus philenor</i> egg clusters are brick-red
         * and chemically defended by maternal transfer.
         */
        MATERNAL_TRANSFER
    }
}
