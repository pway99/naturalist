package com.naturalist.insects.lifestage;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.MonthDay;
import java.util.List;
import java.util.function.Consumer;

/**
 * Seasonal activity pattern for a life stage. Multi-window to support multi-peak
 * flights and split cohorts (e.g. direct-developer vs. diapauser pupae).
 */
public record StagePhenology(
        List<ActivityWindow> windows,
        @Nullable String notes
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .valueObjectCollection(this, StagePhenology::windows, "windows")
                .notEmpty(this, StagePhenology::windows, "windows");
    }

    /**
     * A single coherent activity band. {@code cohortLabel} distinguishes windows
     * when multiple windows describe semantically distinct cohorts rather than
     * repeated peaks of the same cohort.
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
        }
    }
}
