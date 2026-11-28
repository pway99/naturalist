package com.naturalist.observability.constraints;

import com.naturalist.ddd.Enums;
import com.naturalist.observability.Constraint;
import org.apache.commons.lang3.StringUtils;

/**
 * A stored or transmitted name of a constant of {@code type}: valid when blank (null, empty or
 * whitespace, which says "absent") or when it is exactly the {@link Enum#name()} of one of
 * {@code type}'s constants. Pair with {@code notBlank} when the value is required.
 */
public record EnumValueConstraint<E extends Enum<E>>(
        String value,
        Class<E> type,
        String name
) implements Constraint<String> {

    @Override
    public boolean isValid() {
        return StringUtils.isBlank(value) || Enums.named(type, value) != null;
    }

    @Override
    public String errorMessage() {
        return "'%s' names no %s".formatted(value, type.getSimpleName());
    }

    public EnumValueConstraint<E> withName(String name) {
        return new EnumValueConstraint<>(value, type, name);
    }
}
