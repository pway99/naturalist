package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * The optimum range a lab printed beside a measurement — the "Optimum Range" column of an FGL
 * report. A faithful record of what the report said, in the shape the report said it.
 * <p>
 * Sealed because the column has genuinely different shapes and flattening them loses information.
 * A row printed {@code < 19} is not a closed range with an invented zero floor, and a row printed
 * {@code ---} is not a range at all. Modelling all three as (min, max) with nulls would force
 * every consumer to re-derive which case it is from which fields happen to be null.
 * <p>
 * <b>Shapes on the March 2026 reports:</b> {@link Closed} for fifteen of the seventeen nutrients
 * ({@code 5.3 - 7.2}); {@link UpperBounded} for soluble sodium ({@code < 19} on Box 1,
 * {@code < 26} on the back yard); {@link NotApplicable} for the {@code ---} rows in the
 * requirements block. {@link LowerBounded} does not appear on either March report — it is included
 * for symmetry and because a lab that prints a floor-only row is entirely ordinary. Nothing in the
 * fixtures exercises it yet.
 * <p>
 * <b>This is measured-adjacent fact, not judgment.</b> The range is what the lab printed; whether
 * a given reading sits inside it, and what to do about it, is interpretation and belongs to the
 * agronomy effort applied over these values. In particular this type deliberately has no
 * {@code contains} predicate — {@link NotApplicable} has no honest answer to it, and a method that
 * quietly returns {@code false} for "there is no range" is a bug waiting to be written.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "shape")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OptimumRange.Closed.class, name = "CLOSED"),
        @JsonSubTypes.Type(value = OptimumRange.UpperBounded.class, name = "UPPER_BOUNDED"),
        @JsonSubTypes.Type(value = OptimumRange.LowerBounded.class, name = "LOWER_BOUNDED"),
        @JsonSubTypes.Type(value = OptimumRange.NotApplicable.class, name = "NOT_APPLICABLE")
})
public sealed interface OptimumRange extends ValueObject {

    /** Both bounds printed: {@code 5.3 - 7.2}. Inclusive at both ends, as the report reads. */
    record Closed(BigDecimal min, BigDecimal max) implements OptimumRange {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .notNull(min, "min")
                    .notNull(max, "max")
                    .isTrue(min == null || max == null || min.compareTo(max) <= 0, "minNotAboveMax");
        }
    }

    /** A ceiling only: {@code < 19}. The report states no floor, so neither do we. */
    record UpperBounded(BigDecimal max) implements OptimumRange {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.notNull(max, "max");
        }
    }

    /** A floor only: {@code > 5}. Not present on the March reports; see the type javadoc. */
    record LowerBounded(BigDecimal min) implements OptimumRange {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.notNull(min, "min");
        }
    }

    /**
     * The report printed {@code ---}: no optimum applies to this row. A distinct fact from "we
     * did not transcribe it", which is why it is a value rather than a null.
     */
    record NotApplicable() implements OptimumRange {

        /** Nothing to constrain — the absence of an optimum is the whole content of this value. */
        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> {
            };
        }
    }
}
