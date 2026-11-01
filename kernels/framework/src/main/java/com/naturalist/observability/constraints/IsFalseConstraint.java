package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;

/**
 * Generic boolean predicate constraint — the negation of {@link IsTrueConstraint}: fires a
 * violation when {@code value} is {@code true}. Lets a call site keep the constraint's name
 * equal to a positively-phrased predicate method ({@code isConsumed}, {@code isExpired}) so
 * the metric tag names the method a reviewer can read, instead of a negated paraphrase.
 *
 * <p>The predicate is computed by the caller before constructing the constraint; this type
 * does not carry a function reference. Compose with
 * {@link com.naturalist.observability.Constraints#whenNotNull} when the predicate depends on
 * a nullable field's presence.
 */
public record IsFalseConstraint(Boolean value, String name) implements Constraint<Boolean> {
    @Override
    public Boolean value() {
        return value;
    }

    @Override
    public boolean isValid() {
        return !value;
    }

    @Override
    public IsFalseConstraint withName(String name) {
        return new IsFalseConstraint(value, name);
    }
}
