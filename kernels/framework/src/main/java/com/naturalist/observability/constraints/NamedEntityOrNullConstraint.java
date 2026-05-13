package com.naturalist.observability.constraints;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Constraint;
import com.naturalist.observability.ConstraintCollection;
import com.naturalist.observability.Constraints;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Constraint for a nullable {@link Named} component — the canonical encoding of
 * "this child entity is either null or a meaningful {@code NamedEntity} /
 * {@code Entity}."
 * <p>
 * A null reference passes: the field's {@code @Nullable} declaration is respected,
 * and the absent state carries no invariants. A non-null reference is descended
 * into, surfacing the referenced entity's own {@link com.naturalist.observability.Observable#invariants()}
 * to the graph walker.
 * <p>
 * <b>Meaningfulness is the child's responsibility.</b> This constraint guarantees the
 * child's invariants are actually walked when the reference is present, so a
 * semantically-empty entity cannot hide behind a silent parent. It is up to the
 * referenced {@code Named} to include invariants that reject its vacuous-empty
 * form. The parent should carry {@code null} rather than a non-null-but-empty
 * reference.
 * <p>
 * Distinct from {@link ObservableConstraint} — which requires a non-null reference —
 * as its own type rather than a boolean flag so the intent is visible at call sites
 * and in stack traces, and so review tools can grep for the pattern. Sibling to
 * {@link ValueObjectOrNullConstraint}, which carries the same shape for the value-
 * object branch.
 */
public record NamedEntityOrNullConstraint<O, V extends Named<?>>(
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
    public NamedEntityOrNullConstraint<O, V> withName(String name) {
        return new NamedEntityOrNullConstraint<>(o, valueFunction, name);
    }
}
