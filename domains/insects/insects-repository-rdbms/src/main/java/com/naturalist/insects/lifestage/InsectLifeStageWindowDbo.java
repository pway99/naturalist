package com.naturalist.insects.lifestage;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.time.MonthDay;
import java.util.function.Consumer;

/**
 * Child row for one {@link StagePhenology.ActivityWindow} of a life stage, ordered within its parent by
 * {@code ordinal}. Carries the life stage's NAME (JOIN-projected on read, nested-selected to the id on
 * write). {@code MonthDay} values store as their {@code --MM-DD} text form; {@code peak} is nullable.
 */
@DboSchema(table = "insect_life_stage_window", primaryKey = "life_stage_id,ordinal",
           foreignKeys = @Fk(columns = "life_stage_id", references = "insect_life_stage(id)"),
           entity = LifeStage.class)
final class InsectLifeStageWindowDbo implements Dbo {
    String lifeStageName;  // JOIN projection / nested-select key; not a stored column
    int ordinal;
    String onset;
    String peak;           // nullable
    String tail;
    String cohortLabel;    // nullable

    static InsectLifeStageWindowDbo from(String lifeStageName, int ordinal, StagePhenology.ActivityWindow w) {
        InsectLifeStageWindowDbo d = new InsectLifeStageWindowDbo();
        d.lifeStageName = lifeStageName;
        d.ordinal = ordinal;
        d.onset = w.onset().toString();
        d.peak = w.peak() == null ? null : w.peak().toString();
        d.tail = w.tail().toString();
        d.cohortLabel = w.cohortLabel();
        Observer.forClass(InsectLifeStageWindowDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    StagePhenology.ActivityWindow toWindow() {
        return new StagePhenology.ActivityWindow(
                MonthDay.parse(onset),
                peak == null ? null : MonthDay.parse(peak),
                MonthDay.parse(tail),
                cohortLabel);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(lifeStageName, "lifeStageName").kebabFormat(lifeStageName, "lifeStageName")
                .notBlank(onset, "onset").notBlank(tail, "tail");
    }
}
