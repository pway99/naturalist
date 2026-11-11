package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;

import java.util.function.Function;

/**
 * Null-tolerant counterpart of {@link InRangeConstraint}: a null value passes (the
 * component's {@code @Nullable} declaration is respected); a present value must lie
 * within the same bounds. Its own type rather than a flag, so the intent is visible at
 * call sites and in stack traces, as with {@link ValueObjectOrNullConstraint}.
 */
public record InRangeOrNullConstraint<T, V extends Comparable<V>>(
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
        return v == null || InRangeConstraint.within(v, min, minExclusive, max);
    }

    @Override
    public InRangeOrNullConstraint<T, V> withName(String name) {
        return new InRangeOrNullConstraint<>(o, valueFunction, min, minExclusive, max, name);
    }

    @Override
    public String errorMessage() {
        return InRangeConstraint.rangeMessage(min, minExclusive, max);
    }
}
