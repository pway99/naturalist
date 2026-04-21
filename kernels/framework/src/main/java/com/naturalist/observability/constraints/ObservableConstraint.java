package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;
import com.naturalist.observability.ConstraintCollection;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Composite constraint for a non-null {@link Observable} member — an {@code Entity},
 * {@code Aggregate}, or {@code ValueObject}. Acts as both:
 * <ul>
 *   <li>a leaf {@link Constraint} asserting the member reference is present, and</li>
 *   <li>a {@link ConstraintCollection} exposing the referenced observable's own
 *       constraints so the graph walker can descend one level.</li>
 * </ul>
 * A {@code null} member fails {@link #isValid()} and contributes no children. The
 * nullable counterpart is {@link ValueObjectOrNullConstraint}, which permits a null
 * reference and descends only when the reference is present.
 */
public record ObservableConstraint<O, V extends Observable>(
        O o,
        Function<O, V> valueFunction,
        String name
) implements Constraint<V>, ConstraintCollection {

    @Override
    public V value() {
        return o == null ? null : valueFunction.apply(o);
    }

    @Override
    public boolean isValid() {
        if (o == null) {
            return false;
        }
        return valueFunction.apply(o) != null;
    }

    @Override
    public Set<Constraint<?>> constraints() {
        if (o == null) {
            return Set.of();
        }
        V v = valueFunction.apply(o);
        if (v == null) {
            return Set.of();
        }
        Constraints accumulator = new Constraints();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Consumer<Constraints> consumer = (Consumer) v.invariants();
        consumer.accept(accumulator);
        return new LinkedHashSet<>(accumulator.collected());
    }

    @Override
    public ObservableConstraint<O, V> withName(String name) {
        return new ObservableConstraint<>(o, valueFunction, name);
    }
}
