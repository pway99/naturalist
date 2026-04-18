package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

public record NotBlankConstraint<T>(
        T o,
        Function<T, String> valueFunction,
        String name
) implements Constraint<String> {

    @Override
    public String value() {
        return valueFunction.apply(o);
    }

    @Override
    public boolean isValid() {
        return StringUtils.isNotBlank(valueFunction.apply(o));
    }

    public NotBlankConstraint<T> withName(String name) {
        return new NotBlankConstraint<>(o, valueFunction, name);
    }
}