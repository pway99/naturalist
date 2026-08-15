package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * How much of something a lab recommends applying — the amount column of an FGL recommendation
 * row, in the shape the report printed it.
 * <p>
 * <b>{@link None} is a value, not a missing row.</b> "Apply no nitrogen" is a recommendation the
 * lab made; it is not the absence of a recommendation, and a model that represents it as null
 * cannot tell the two apart. Box 1's report prints eight explicit {@code None} rows, and reading
 * them as "unknown" would let a fertiliser program invent applications the lab advised against.
 * <p>
 * <b>{@link Quantity} of zero is a third thing again.</b> Box 1's Lime Requirement is printed as
 * {@code 0 Tons/AF} — a computed requirement that came out at zero — while its Lime
 * <em>fertilisation</em> row is printed as {@code None}. Same report, same input, two different
 * statements: the soil needs no lime, and no lime is being recommended. Both are preserved.
 * <p>
 * <b>{@link BelowDetectionLimit}</b> carries {@code < 0.50 Tons/AF}, printed for the Gypsum
 * Requirement on both March reports. This is the second censored value in the domain — the first
 * is {@code CationBaseSaturation.hydrogenBelowDetectionLimit} — and the two are represented
 * differently on purpose for now: hydrogen's censoring needs sum bounds, this one needs a "None"
 * sibling, and merging them into one shared quantity type is a design question rather than a
 * rename. See the follow-up noted in the plan.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RecommendedAmount.Quantity.class, name = "QUANTITY"),
        @JsonSubTypes.Type(value = RecommendedAmount.None.class, name = "NONE"),
        @JsonSubTypes.Type(value = RecommendedAmount.BelowDetectionLimit.class, name = "BELOW_DETECTION_LIMIT")
})
public sealed interface RecommendedAmount extends ValueObject {

    /** A printed amount, in the row's unit. Zero is a legitimate value — see the type javadoc. */
    record Quantity(BigDecimal value) implements RecommendedAmount {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .notNull(value, "value")
                    .isTrue(value == null || value.signum() >= 0, "notNegative");
        }
    }

    /** The report printed {@code None}: the lab recommends applying none of this input. */
    record None() implements RecommendedAmount {

        /** Nothing to constrain — "apply none" is complete in itself. */
        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> {
            };
        }
    }

    /**
     * The report printed {@code < limit}: the requirement is real but below what the method
     * resolves. The true amount lies in {@code [0, limit)}, and is not the same as {@link None}.
     */
    record BelowDetectionLimit(BigDecimal limit) implements RecommendedAmount {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .notNull(limit, "limit")
                    .isTrue(limit == null || limit.signum() > 0, "positiveLimit");
        }
    }
}
