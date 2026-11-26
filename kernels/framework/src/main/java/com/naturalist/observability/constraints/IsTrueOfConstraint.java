package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * By-function form of {@link IsTrueConstraint}: fires a violation when {@code predicate} does not hold for
 * {@code o}. The predicate runs only when the constraint is evaluated and only on a non-null {@code o}; a null
 * {@code o} passes, since its absence is the job of the constraint that checks {@code o} itself
 * ({@code readModel}, {@code namedEntity}, {@code notNull}), so the null is counted once.
 */
public record IsTrueOfConstraint<T>(
        @Nullable T o,
        Predicate<T> predicate,
        String name
) implements Constraint<Boolean> {

    @Override
    public Boolean value() {
        return isValid();
    }

    @Override
    public boolean isValid() {
        return o == null || predicate.test(o);
    }

    public IsTrueOfConstraint<T> withName(String name) {
        return new IsTrueOfConstraint<>(o, predicate, name);
    }
}
