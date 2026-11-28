package com.naturalist.insects.lifestage;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.time.Month;
import java.time.MonthDay;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    Integer ordinal;
    String onset;
    String peak;           // nullable
    String tail;
    String cohortLabel;    // nullable

    private static final Observer observer = Observer.forClass(InsectLifeStageWindowDbo.class);

    /** The {@code --MM-DD} text form {@link MonthDay#toString()} writes. */
    private static final Pattern MONTH_DAY = Pattern.compile("--(\\d{2})-(\\d{2})");

    static InsectLifeStageWindowDbo from(String lifeStageName, Integer ordinal, StagePhenology.ActivityWindow window) {
        observer.arguments("from", i -> i
                .kebabFormat(lifeStageName, "lifeStageName")
                .atLeast(ordinal, 0, "ordinal")
                .valueObject(window, "window"))
                .throwWhenInvalid();
        InsectLifeStageWindowDbo d = new InsectLifeStageWindowDbo();
        d.lifeStageName = lifeStageName;
        d.ordinal = ordinal;
        d.onset = window.onset().toString();
        d.peak = window.peak() == null ? null : window.peak().toString();
        d.tail = window.tail().toString();
        d.cohortLabel = window.cohortLabel();
        return d;
    }

    StagePhenology.ActivityWindow toWindow() {
        return new StagePhenology.ActivityWindow(
                monthDayOrNull(onset),
                monthDayOrNull(peak),
                monthDayOrNull(tail),
                cohortLabel);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .kebabFormat(lifeStageName, "lifeStageName")
                .atLeast(ordinal, 0, "ordinal")
                .notBlank(onset, "onset")
                .notBlank(tail, "tail")
                .isTrue(isMonthDay(onset), "onsetIsMonthDay")
                .isTrue(isMonthDay(tail), "tailIsMonthDay")
                .whenNotNull(peak, p -> p.isTrue(isMonthDay(peak), "peakIsMonthDay"));
    }

    /** {@code text} as a {@link MonthDay}, or null when it is not one. Never throws. */
    private static MonthDay monthDayOrNull(String text) {
        return isMonthDay(text) ? MonthDay.parse(text) : null;
    }

    /** Whether {@code text} is a real day of the year in {@code --MM-DD} form; null is not. Never throws. */
    private static boolean isMonthDay(String text) {
        if (text == null) return false;
        Matcher matcher = MONTH_DAY.matcher(text);
        if (!matcher.matches()) return false;
        int month = Integer.parseInt(matcher.group(1));
        int day = Integer.parseInt(matcher.group(2));
        return month >= 1 && month <= 12 && day >= 1 && day <= Month.of(month).maxLength();
    }
}
