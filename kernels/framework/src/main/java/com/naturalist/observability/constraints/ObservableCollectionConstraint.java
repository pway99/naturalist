package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;
import com.naturalist.observability.ConstraintCollection;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Constraint for a {@link Collection} of {@link Observable} elements: the collection
 * reference is non-null, and every element is non-null and satisfies its own
 * {@link Observable#invariants()}.
 * <p>
 * Descends into each element's invariants for field-level attribution, but names the
 * descended constraints by field ONLY — with no element-index segment. An invalid
 * element surfaces as e.g. {@code rankNames.rankName}, never {@code rankNames.[7].rankName}:
 * the field is bounded, low-cardinality signal worth keeping; the element index is
 * unbounded metric noise. Only invalid descended constraints are returned, deduped by
 * name — a field failed by many elements collapses to a single violation entry, so
 * failures across multiple elements collapse to one field-level violation name no
 * matter how many elements failed it. Empty collection is valid (pair with
 * {@code notEmpty}).
 */
public record ObservableCollectionConstraint<O extends Observable>(
        Collection<? extends O> value,
        String name
) implements Constraint<Collection<? extends O>>, ConstraintCollection {

    @Override
    public boolean isValid() {
        if (value == null) {
            return false;
        }
        for (O element : value) {
            if (element == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<Constraint<?>> constraints() {
        if (value == null) {
            return Set.of();
        }
        // Only the INVALID descended constraints, keyed by name so each distinct
        // constraint path appears once — a field failed by many elements collapses to a
        // single violation (e.g. rankNames.rankName), and the element index never enters
        // the name. Valid elements contribute nothing.
        Map<String, Constraint<?>> invalidByName = new LinkedHashMap<>();
        for (O element : value) {
            if (element == null) {
                continue;
            }
            Constraints accumulator = new Constraints();
            @SuppressWarnings({"unchecked", "rawtypes"})
            Consumer<Constraints> consumer = (Consumer) element.invariants();
            consumer.accept(accumulator);
            for (Constraint<?> child : accumulator.collected()) {
                if (!child.isValid()) {
                    invalidByName.putIfAbsent(child.name(), child);
                }
                if (child instanceof ConstraintCollection nested) {
                    for (Constraint<?> descendant : nested.collectConstraints()) {
                        if (!descendant.isValid()) {
                            invalidByName.putIfAbsent(descendant.name(), descendant);
                        }
                    }
                }
            }
        }
        return new LinkedHashSet<>(invalidByName.values());
    }

    @Override
    public ObservableCollectionConstraint<O> withName(String name) {
        return new ObservableCollectionConstraint<>(value, name);
    }
}
