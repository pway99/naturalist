package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;
import java.util.function.Function;

/**
 * Asserts a string's length does not exceed {@code max} (inclusive), mirroring a
 * {@code VARCHAR(max)} column width. Null-tolerant: a null value passes — pair with
 * {@link com.naturalist.observability.Constraints#notNull} when presence is also required.
 */
public record StringLengthLessThanConstraint<T>(
        T o,
        Function<T, String> valueFunction,
        int max,
        String name
) implements Constraint<String> {

    @Override
    public String value() {
        return o == null ? null : valueFunction.apply(o);
    }

    @Override
    public boolean isValid() {
        String v = value();
        return v == null || v.length() <= max;
    }

    @Override
    public StringLengthLessThanConstraint<T> withName(String name) {
        return new StringLengthLessThanConstraint<>(o, valueFunction, max, name);
    }

    @Override
    public String errorMessage() {
        String v = value();
        return v == null ? "" : "Length %d exceeds max %d".formatted(v.length(), max);
    }
}
