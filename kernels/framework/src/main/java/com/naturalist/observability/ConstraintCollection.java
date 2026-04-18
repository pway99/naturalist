package com.naturalist.observability;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Composite node in the constraint graph.
 * <p>
 * A {@code ConstraintCollection} exposes child constraints that {@link #collectConstraints()}
 * will descend into, producing a flattened set where each constraint name is qualified by
 * its path through the graph using dotted notation (e.g. {@code compoundInfo.molecularWeight}).
 * <p>
 * Each recursion level prepends exactly one segment — the collection's own {@link #name()} —
 * to each child's name. When a child is itself a {@code ConstraintCollection}, its flattened
 * descendants (already qualified by the child's name) are further prepended, composing the
 * fully-qualified path one segment per level.
 */
public interface ConstraintCollection {

    String name();

    Set<Constraint<?>> constraints();

    default Set<Constraint<?>> collectConstraints() {
        Set<Constraint<?>> result = new LinkedHashSet<>();
        String prefix = name() + ".";
        for (Constraint<?> constraint : constraints()) {
            result.add(constraint.withName(prefix + constraint.name()));
            if (constraint instanceof ConstraintCollection nested) {
                for (Constraint<?> child : nested.collectConstraints()) {
                    result.add(child.withName(prefix + child.name()));
                }
            }
        }
        return result;
    }
}