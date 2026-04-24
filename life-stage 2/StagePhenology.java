package com.naturalist.insects.lifestages;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.MonthDay;
import java.util.List;
import java.util.function.Consumer;

/**
 * The seasonal activity pattern of a life stage at Oak Vista.
 * <p>
 * Phenology is stage-specific. The adult flight period of <i>Battus philenor</i>
 * (February through October with multiple peaks and an August emergence bump from
 * diapause breakage) is a distinct pattern from the larval feeding window or the
 * pupal development window. Each stage carries its own {@code StagePhenology}.
 * <p>
 * The primary modelling decision is that phenology is <b>structured, not prose</b>.
 * The current {@code AdultStage.flightPeriod} field is a single narrative string;
 * that collapses real structure — onset, peaks, end — into text that consumers
 * must re-parse to reason about. The DAG layer, in particular, will want to query
 * phenology as structure ("which stages are active in July?", "which species have
 * overlapping adult flight periods with bee emergence?") and those queries cannot
 * run against narrative.
 * <p>
 * The shape below is a compromise between pure structure and pragmatic field-notes
 * reality: the {@link ActivityWindow} captures the onset/peak/tail shape of a
 * single activity band, and {@code windows} is a list because many species have
 * multi-peak flights or split cohorts. {@code notes} carries the narrative context
 * that doesn't fit structured fields — why the pattern looks the way it does, what
 * drives interannual variation, anything a naturalist would want to record beyond
 * the raw dates.
 * <p>
 * <b>Split-diapause case:</b> <i>Battus philenor</i> pupae produce both direct-
 * developing and diapausing cohorts from a single clutch. The pupal phenology
 * carries two windows — the direct-developer window (summer pupation to same-year
 * eclosion, a few weeks) and the diapauser window (summer pupation to next-spring
 * eclosion, several months). {@link PupaStage} may additionally carry a
 * {@code DiapauseRegulation} value explaining the mechanism; phenology captures
 * only the <i>observable</i> timing structure.
 * <p>
 * {@link MonthDay} is used deliberately rather than {@code LocalDate} — phenology
 * is year-agnostic. <i>Battus philenor</i> adults emerge around mid-February every
 * year; the specific year doesn't belong in the phenology. Interannual variation
 * belongs in observation records, not in the catalog-level phenology.
 */
public record StagePhenology(
        List<ActivityWindow> windows,
        @Nullable String notes
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(this, StagePhenology::windows, "windows")
                // A present phenology with no windows is meaningless — if phenology
                // is genuinely unknown, the containing stage should not yet declare
                // one. Empty list is an invariant violation, not a degenerate case.
                .notEmpty(this, StagePhenology::windows, "windows");
    }

    /**
     * A single coherent activity band — one flight, one cohort, one emergence
     * wave. Species with multi-peak phenology carry multiple windows; species
     * with split cohorts (direct-developer vs. diapauser) carry one window per
     * cohort.
     * <p>
     * {@code onset} and {@code tail} bracket the window. {@code peak} is the
     * observed peak activity date within the window; nullable for windows where
     * activity is even or too diffuse to call a peak. {@code cohortLabel} names
     * the window when multiple windows on the same stage describe semantically
     * distinct cohorts — for the split-diapause case, {@code "direct-developer"}
     * and {@code "diapauser"}. Nullable for single-window phenologies and for
     * multi-peak phenologies where the peaks are the same cohort behaving the
     * same way at different times.
     */
    public record ActivityWindow(
            MonthDay onset,
            @Nullable MonthDay peak,
            MonthDay tail,
            @Nullable String cohortLabel
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .notNull(this, ActivityWindow::onset, "onset")
                    .notNull(this, ActivityWindow::tail, "tail");
            // Note: onset-before-tail and peak-within-window are checks that would
            // naturally live here, but MonthDay comparison wraps around year
            // boundaries — an overwintering window with onset in October and tail
            // in March is legitimate. The ordering check is therefore non-trivial
            // and deferred until we have a concrete CalendarRange type or equivalent
            // in the kernel. For now, the invariants are the presence checks.
        }
    }
}
