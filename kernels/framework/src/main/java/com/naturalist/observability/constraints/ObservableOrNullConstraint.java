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
 * Null-tolerant counterpart of {@link ObservableConstraint}, for a {@code @Nullable}
 * {@code ReadModel} or other {@link Observable} member that is neither a named entity,
 * an aggregate nor a value object (each of which has its own {@code OrNull} type). A null
 * reference passes; a non-null reference is descended into, surfacing its own
 * {@code invariants()} to the graph walker.
 */
public record ObservableOrNullConstraint<O, V extends Observable>(
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
        // A null child is valid by contract; a non-null child is judged by its own
        // invariants, which the graph walker descends into via constraints().
        return true;
    }

    @Override
    public Set<Constraint<?>> constraints() {
        V v = value();
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
    public ObservableOrNullConstraint<O, V> withName(String name) {
        return new ObservableOrNullConstraint<>(o, valueFunction, name);
    }
}
