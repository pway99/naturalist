package com.naturalist.observability.constraints;

import com.naturalist.ddd.Aggregate;
import com.naturalist.observability.Constraint;
import com.naturalist.observability.ConstraintCollection;
import com.naturalist.observability.Constraints;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Constraint for a nullable {@link Aggregate} component — the canonical encoding of
 * "this child aggregate is either null or a meaningful aggregate."
 * <p>
 * A null reference passes: the field's {@code @Nullable} declaration is respected, and
 * the absent state carries no invariants. A non-null reference is descended into,
 * surfacing the referenced aggregate's own {@link Aggregate#invariants()} to the
 * graph walker.
 * <p>
 * <b>Meaningfulness is the child's responsibility.</b> This constraint guarantees the
 * child's invariants are actually walked when the reference is present, so a
 * semantically-empty aggregate cannot hide behind a silent parent.
 * <p>
 * Distinct from {@link ObservableConstraint} — which requires a non-null reference —
 * as its own type rather than a boolean flag so the intent is visible at call sites
 * and in stack traces, and so review tools can grep for the pattern. Sibling to
 * {@link ValueObjectOrNullConstraint} and {@link NamedEntityOrNullConstraint}, which
 * carry the same shape for their respective branches.
 */
public record AggregateOrNullConstraint<O, V extends Aggregate>(
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
        return true;
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
    public AggregateOrNullConstraint<O, V> withName(String name) {
        return new AggregateOrNullConstraint<>(o, valueFunction, name);
    }
}
