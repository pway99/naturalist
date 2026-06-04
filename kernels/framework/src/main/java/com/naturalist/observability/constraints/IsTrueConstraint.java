package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;

/**
 * Generic boolean predicate constraint — fires a violation when {@code value}
 * is {@code false}. Use for invariants that don't fit any of the
 * type-specific constraints (e.g. cross-field consistency rules expressed as
 * a domain-specific predicate method on the owning record).
 *
 * <p>The predicate is computed by the caller before constructing the
 * constraint; this type does not carry a function reference. Compose with
 * {@link com.naturalist.observability.Constraints#whenNotNull} when the
 * predicate depends on a nullable field's presence.
 */
public record IsTrueConstraint(
        boolean value,
        String name
) implements Constraint<Boolean> {

    @Override
    public Boolean value() {
        return value;
    }

    @Override
    public boolean isValid() {
        return value;
    }

    public IsTrueConstraint withName(String name) {
        return new IsTrueConstraint(value, name);
    }
}
