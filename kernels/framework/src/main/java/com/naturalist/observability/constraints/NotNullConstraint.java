package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;

import java.util.function.Function;

public record NotNullConstraint<T, R>(
        T o,
        Function<T, R> valueFunction,
        String name
) implements Constraint<R> {

    @Override
    public R value() {
        return valueFunction.apply(o);
    }

    @Override
    public boolean isValid() {
        return valueFunction.apply(o) != null;
    }

    public NotNullConstraint<T, R> withName(String name) {
        return new NotNullConstraint<>(o, valueFunction, name);
    }
}