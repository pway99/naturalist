package com.naturalist.soil.observation;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Where a measurement sits relative to the optimum range the lab printed beside it — and who says
 * so. Always carries its {@link AssessmentSource}, because this comparison is <em>ours</em>, not
 * the lab's verdict.
 * <p>
 * <b>Three positions, never a position within a band.</b> The report's graphical block sorts each
 * row into five bands (Very Low … Very High) and draws a bar somewhere inside one. We store
 * neither the band boundaries nor the bar position, so we can say "below / within / above the
 * printed range" and nothing finer. Rendering a bar position, a percentage-through-band, or a
 * five-band label would be inventing precision the stored data does not contain — and the report's
 * own bars sometimes disagree with plain range arithmetic (see {@link AssessmentSource}).
 * <p>
 * {@link Verdict#NOT_COMPARABLE} covers every case where the question has no answer: the lab did
 * not report the nutrient, printed no optimum for it, or printed {@code ---}. It is not a fourth
 * position on the scale; it is the absence of a scale.
 */
public record OptimumComparison(Verdict verdict, AssessmentSource source) implements ValueObject {

    public enum Verdict {
        BELOW,
        WITHIN,
        ABOVE,
        /** No comparison is possible — see the type javadoc. Never render this as a judgment. */
        NOT_COMPARABLE
    }

    private static final OptimumComparison NOT_COMPARABLE =
            new OptimumComparison(Verdict.NOT_COMPARABLE, AssessmentSource.DERIVED_FROM_PRINTED_RANGE);

    /** The comparison that cannot be made. */
    public static OptimumComparison notComparable() {
        return NOT_COMPARABLE;
    }

    /**
     * Compares a reading against an optimum, when both exist. An absent reading, an absent
     * optimum, or a {@link OptimumRange.NotApplicable} range all yield
     * {@link Verdict#NOT_COMPARABLE}.
     */
    public static OptimumComparison of(Optional<NutrientReading> reading,
                                       Optional<ReportedOptimum> optimum) {
        if (reading.isEmpty() || optimum.isEmpty()) {
            return notComparable();
        }
        return of(reading.get().value(), optimum.get().range());
    }

    /** Bounds are inclusive, as the report reads them. */
    public static OptimumComparison of(BigDecimal value, OptimumRange range) {
        if (value == null || range == null) {
            return notComparable();
        }
        Verdict verdict = switch (range) {
            case OptimumRange.Closed closed -> {
                if (value.compareTo(closed.min()) < 0) {
                    yield Verdict.BELOW;
                }
                yield value.compareTo(closed.max()) > 0 ? Verdict.ABOVE : Verdict.WITHIN;
            }
            case OptimumRange.UpperBounded upper ->
                    value.compareTo(upper.max()) > 0 ? Verdict.ABOVE : Verdict.WITHIN;
            case OptimumRange.LowerBounded lower ->
                    value.compareTo(lower.min()) < 0 ? Verdict.BELOW : Verdict.WITHIN;
            case OptimumRange.NotApplicable ignored -> Verdict.NOT_COMPARABLE;
        };
        return new OptimumComparison(verdict, AssessmentSource.DERIVED_FROM_PRINTED_RANGE);
    }

    /** Whether this says anything at all — false for {@link Verdict#NOT_COMPARABLE}. */
    public boolean isConclusive() {
        return verdict != Verdict.NOT_COMPARABLE;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(verdict, "verdict")
                .notNull(source, "source");
    }
}
