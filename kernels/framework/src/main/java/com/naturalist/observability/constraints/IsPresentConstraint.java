package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;

import java.util.Optional;

/**
 * Asserts an {@link java.util.Optional} holds a value: fires a violation when it is empty or
 * null. Use where an acquired lookup must have found something before the method can go on,
 * so the observation names what was missing instead of an {@code isTrue(found.isPresent(), ...)}.
 */
public record IsPresentConstraint(Optional<?> value, String name) implements Constraint<Optional<?>> {
    @Override
    public boolean isValid() {
        return value != null && value.isPresent();
    }

    @Override
    public IsPresentConstraint withName(String name) {
        return new IsPresentConstraint(value, name);
    }
}
