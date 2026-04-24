package com.naturalist.insects.lifestages;

import com.naturalist.ddd.ValueObject;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The pupal stage of a holometabolous insect's life cycle — the immobile
 * transformative stage between larva and adult.
 * <p>
 * Only present on species undergoing complete metamorphosis (Holometabola):
 * Neuroptera, Coleoptera, Diptera, Hymenoptera, Lepidoptera. Hemimetabolous orders
 * (Orthoptera, Hemiptera, Blattodea) do not pupate; on those species the species
 * aggregate's {@code pupa} field is {@code null}, not an absent-but-unknown
 * {@code PupaStage}.
 * <p>
 * Beyond the shared {@link LifeStage} contract, the pupa carries two classes of
 * stage-specific structure:
 * <ul>
 *   <li>{@code appearance} — morphological detail and polymorphism. The
 *       <i>Battus philenor</i> chrysalis is dimorphic brown/green with a golden
 *       filigree; this is a species-level fact worth recording.</li>
 *   <li>{@code diapauseRegulation} — the mechanism by which the pupa enters and
 *       exits dormancy. A sealed hierarchy (see {@link DiapauseRegulation}) because
 *       the mechanism space is closed but heterogeneous: photoperiod-regulated
 *       diapause has different fields than food-water-content-regulated diapause,
 *       and collapsing them into nullable strings loses the structure.</li>
 *   <li>{@code adaptiveSignificance} — species-level pupal adaptations worth
 *       recording at catalog level, such as split diapause strategies that produce
 *       indeterminate voltinism. Mirrors the equivalent field on {@link EggStage}.</li>
 * </ul>
 * <p>
 * Note what is <i>not</i> here: the direct-developer vs. diapauser cohort timing
 * lives in {@link StagePhenology} as two {@link StagePhenology.ActivityWindow}s
 * with distinct {@code cohortLabel}s. Phenology is timing; diapause regulation is
 * mechanism; the pupa record carries both, each in its own value object.
 */
public record PupaStage(
        LifeStageName name,
        StagePhenology phenology,
        StageHabitat habitat,
        @Nullable StageChemistryRole chemistryRole,
        Description description,
        @Nullable String appearance,
        @Nullable DiapauseRegulation diapauseRegulation,
        @Nullable String adaptiveSignificance
) implements LifeStage {

    @Override
    public LifeStageKind kind() {
        return LifeStageKind.PUPA;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .valueObject(phenology, "phenology")
                .valueObject(habitat, "habitat")
                .valueObjectOrNull(this, PupaStage::chemistryRole, "chemistryRole")
                .valueObject(description, "description")
                .valueObjectOrNull(this, PupaStage::diapauseRegulation, "diapauseRegulation");
    }

    /**
     * The mechanism governing a pupa's entry into and exit from diapause —
     * developmental dormancy that lets the stage overwinter or extend across a
     * hostile season.
     * <p>
     * Sealed because the space of biologically plausible mechanisms is closed but
     * structurally heterogeneous. Photoperiod-regulated diapause — the temperate
     * default — needs a critical daylength. Food-water-content regulation — the
     * <i>Battus philenor</i> case, where larval food moisture determines whether
     * the pupa direct-develops or diapauses — has no daylength at all. Modelling
     * both as a single record with nullable fields per mechanism loses the guarantee
     * that each case carries the right fields.
     * <p>
     * If a new mechanism appears (temperature-threshold-only, humidity-gated, chemical
     * signalling from conspecifics), this hierarchy gains a subtype. The sealed
     * declaration means every downstream pattern match — including the DAG layer's
     * edge extraction — fails compilation until the new case is handled.
     */
    public sealed interface DiapauseRegulation extends ValueObject
            permits PhotoperiodRegulated,
                    FoodWaterContentRegulated,
                    TemperatureRegulated,
                    NonDiapausing {
    }

    /**
     * The temperate-default mechanism: diapause induction is triggered by
     * shortening daylength falling below a critical threshold. Exit is governed
     * by accumulated chill units followed by warming daylength.
     */
    public record PhotoperiodRegulated(
            @Nullable String criticalDaylength,
            @Nullable String chillRequirement
    ) implements DiapauseRegulation {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> {};
        }
    }

    /**
     * The <i>Battus philenor</i> case. Larval food moisture content at the time of
     * pupation determines whether the pupa direct-develops within the same season
     * or enters diapause until the following spring. Decoupled from photoperiod —
     * a single clutch pupating on the same date under the same daylength produces
     * both developmental pathways depending on which leaves individual larvae fed
     * on.
     * <p>
     * Produces site-level {@code INDETERMINATE} voltinism as a direct consequence:
     * the species cannot be summarised as univoltine or bivoltine because the
     * mechanism itself generates variability within a cohort.
     */
    public record FoodWaterContentRegulated(
            @Nullable String mechanism,
            @Nullable String cohortSplitNotes
    ) implements DiapauseRegulation {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> {};
        }
    }

    /**
     * Diapause entry and exit are governed by accumulated temperature thresholds
     * (degree-days, chill units) without meaningful photoperiodic input. Common in
     * species that diapause in response to seasonal heat or cold extremes rather
     * than predictable daylength change.
     */
    public record TemperatureRegulated(
            @Nullable String entryThreshold,
            @Nullable String exitThreshold
    ) implements DiapauseRegulation {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> {};
        }
    }

    /**
     * The pupa does not diapause — direct development to adult under all documented
     * conditions at the site. Explicit subtype rather than a null {@code DiapauseRegulation}
     * because "does not diapause" is a positive ecological fact, not an absence of
     * information. A species whose diapause biology is simply undocumented carries
     * {@code null} on {@link PupaStage#diapauseRegulation()}; a species confirmed to
     * direct-develop carries a {@code NonDiapausing} instance.
     */
    public record NonDiapausing() implements DiapauseRegulation {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> {};
        }
    }
}
