package com.naturalist.observability.constraints;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraint;
import com.naturalist.observability.ConstraintCollection;
import com.naturalist.observability.Constraints;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Constraint for a nullable {@link ValueObject} component — the canonical encoding of
 * "this child is either null or a meaningful value object."
 * <p>
 * A null reference passes: the field's {@code @Nullable} declaration is respected, and
 * the absent state carries no invariants. A non-null reference is descended into,
 * surfacing the referenced value object's own {@link ValueObject#invariants()} to the
 * graph walker.
 * <p>
 * <b>Meaningfulness is the child's responsibility.</b> This constraint guarantees the
 * child's invariants are actually walked when the reference is present, so a
 * semantically-empty value object cannot hide behind a silent parent. It is up to the
 * referenced {@code ValueObject} to include invariants that reject its vacuous-empty
 * form (e.g. "at least one narrative field is non-blank", "the features list is
 * non-empty"). Constructing a nullable value object with every component null should
 * be illegal at the child's invariant level; the parent should then carry {@code null}
 * rather than a non-null-but-empty reference.
 * <p>
 * Distinct from {@link ObservableConstraint} — which requires a non-null reference —
 * as its own type rather than a boolean flag so the intent is visible at call sites
 * and in stack traces, and so review tools can grep for the pattern.
 */
public record ValueObjectOrNullConstraint<O, V extends ValueObject>(
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
        // A null child is valid by contract; a non-null child's meaningfulness is
        // asserted by the child's own invariants, which the graph walker descends
        // into via constraints().
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
    public ValueObjectOrNullConstraint<O, V> withName(String name) {
        return new ValueObjectOrNullConstraint<>(o, valueFunction, name);
    }
}
