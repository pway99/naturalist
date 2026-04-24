package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Asserts a collection-shaped value is non-null and contains at least one element.
 * A null reference fails; an empty {@link Collection} or {@link Map} fails;
 * anything else passes. Use alongside a descent constraint (e.g.
 * {@code valueObjectCollection}) when both "must have elements" and "each element
 * is valid" need to hold — the two concerns are separate.
 */
public record NotEmptyConstraint<T, R>(
        T o,
        Function<T, R> valueFunction,
        String name
) implements Constraint<R> {

    @Override
    public R value() {
        return o == null ? null : valueFunction.apply(o);
    }

    @Override
    public boolean isValid() {
        if (o == null) {
            return false;
        }
        R v = valueFunction.apply(o);
        if (v == null) {
            return false;
        }
        if (v instanceof Collection<?> c) {
            return !c.isEmpty();
        }
        if (v instanceof Map<?, ?> m) {
            return !m.isEmpty();
        }
        if (v instanceof CharSequence s) {
            return s.length() > 0;
        }
        return true;
    }

    @Override
    public NotEmptyConstraint<T, R> withName(String name) {
        return new NotEmptyConstraint<>(o, valueFunction, name);
    }
}
