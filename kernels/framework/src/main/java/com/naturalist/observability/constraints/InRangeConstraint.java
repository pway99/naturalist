package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;

import java.util.function.Function;

/**
 * Asserts an extracted {@link Comparable} value lies within an inclusive
 * {@code [min, max]} range. Either bound may be null to express a one-sided
 * constraint (atLeast / atMost). A null value passes — pair with
 * {@code notNull} when presence must also be required.
 */
public record InRangeConstraint<T, V extends Comparable<V>>(
        T o,
        Function<T, V> valueFunction,
        V min,
        V max,
        String name
) implements Constraint<V> {

    @Override
    public V value() {
        return valueFunction.apply(o);
    }

    @Override
    public boolean isValid() {
        V v = valueFunction.apply(o);
        if (v == null) return true;
        if (min != null && v.compareTo(min) < 0) return false;
        if (max != null && v.compareTo(max) > 0) return false;
        return true;
    }

    @Override
    public InRangeConstraint<T, V> withName(String name) {
        return new InRangeConstraint<>(o, valueFunction, min, max, name);
    }

    @Override
    public String errorMessage() {
        if (min != null && max != null) return "must be in [" + min + ", " + max + "]";
        if (min != null) return "must be at least " + min;
        if (max != null) return "must be at most " + max;
        return "";
    }
}
