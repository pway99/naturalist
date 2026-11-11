package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;

import java.util.function.Function;

/**
 * Asserts an extracted {@link Comparable} value is present and lies within
 * {@code [min, max]}. Either bound may be null to express a one-sided
 * constraint (atLeast / atMost / greaterThan); {@code minExclusive} makes the
 * lower bound strict. A null value fails — the null-tolerant counterpart is
 * {@link InRangeOrNullConstraint}.
 */
public record InRangeConstraint<T, V extends Comparable<V>>(
        T o,
        Function<T, V> valueFunction,
        V min,
        boolean minExclusive,
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
        return v != null && within(v, min, minExclusive, max);
    }

    @Override
    public InRangeConstraint<T, V> withName(String name) {
        return new InRangeConstraint<>(o, valueFunction, min, minExclusive, max, name);
    }

    @Override
    public String errorMessage() {
        return valueFunction.apply(o) == null ? "must not be null" : rangeMessage(min, minExclusive, max);
    }

    static <V extends Comparable<V>> boolean within(V v, V min, boolean minExclusive, V max) {
        if (min != null) {
            int c = v.compareTo(min);
            if (c < 0 || (minExclusive && c == 0)) return false;
        }
        return max == null || v.compareTo(max) <= 0;
    }

    static String rangeMessage(Object min, boolean minExclusive, Object max) {
        if (min != null && max != null) return "must be in " + (minExclusive ? "(" : "[") + min + ", " + max + "]";
        if (min != null) return (minExclusive ? "must be greater than " : "must be at least ") + min;
        if (max != null) return "must be at most " + max;
        return "";
    }
}
