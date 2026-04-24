package com.naturalist.observability.constraints;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraint;
import com.naturalist.observability.ConstraintCollection;
import com.naturalist.observability.Constraints;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Constraint for a {@link Collection} of {@link ValueObject} elements — the canonical
 * encoding of "this field is a non-null collection of value objects, and each element's
 * own invariants must hold."
 * <p>
 * Acts as both:
 * <ul>
 *   <li>a leaf {@link Constraint} asserting the collection reference is non-null
 *       (null fails; empty is valid — use {@code notEmpty} alongside when empty is
 *       illegal), and</li>
 *   <li>a {@link ConstraintCollection} exposing each element's own
 *       {@link ValueObject#invariants()} so the graph walker descends into every
 *       element. Each element contributes its invariants under an indexed child
 *       path ({@code [0]}, {@code [1]}, ...) so divergent failures stay
 *       attributable to a specific element.</li>
 * </ul>
 */
public record ValueObjectCollectionConstraint<O, V extends ValueObject>(
        O o,
        Function<O, Collection<V>> valueFunction,
        String name
) implements Constraint<Collection<V>>, ConstraintCollection {

    @Override
    public Collection<V> value() {
        return o == null ? null : valueFunction.apply(o);
    }

    @Override
    public boolean isValid() {
        if (o == null) {
            return false;
        }
        Collection<V> collection = valueFunction.apply(o);
        if (collection == null) {
            return false;
        }
        for (V element : collection) {
            if (element == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<Constraint<?>> constraints() {
        if (o == null) {
            return Set.of();
        }
        Collection<V> collection = valueFunction.apply(o);
        if (collection == null) {
            return Set.of();
        }
        Set<Constraint<?>> result = new LinkedHashSet<>();
        int index = 0;
        for (V element : collection) {
            String elementName = "[" + index + "]";
            if (element == null) {
                index++;
                continue;
            }
            Constraints accumulator = new Constraints();
            @SuppressWarnings({"unchecked", "rawtypes"})
            Consumer<Constraints> consumer = (Consumer) element.invariants();
            consumer.accept(accumulator);
            for (Constraint<?> child : accumulator.collected()) {
                result.add(child.withName(elementName + "." + child.name()));
                if (child instanceof ConstraintCollection nested) {
                    for (Constraint<?> grand : nested.collectConstraints()) {
                        result.add(grand.withName(elementName + "." + grand.name()));
                    }
                }
            }
            index++;
        }
        return result;
    }

    @Override
    public ValueObjectCollectionConstraint<O, V> withName(String name) {
        return new ValueObjectCollectionConstraint<>(o, valueFunction, name);
    }
}
