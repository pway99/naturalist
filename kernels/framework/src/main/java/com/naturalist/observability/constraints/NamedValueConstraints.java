package com.naturalist.observability.constraints;

import com.naturalist.ddd.NamedValue;
import com.naturalist.observability.Constraint;

import java.util.function.Function;

/**
 * Observability constraint for {@link NamedValue} fields.
 * <p>
 * Delegates validity to {@link NamedValue#isValid()}, consistent with how
 * {@link EntityNameConstraints} handles its field type. The container that owns the
 * field declares the constraint via {@code Constraints#namedValue(...)}; the named
 * value itself is not {@code Observable}.
 */
public interface NamedValueConstraints {

    record NamedValueConstraint<O, V extends NamedValue<?>>(
            O o,
            Function<O, V> valueFunction,
            String name
    ) implements Constraint<V> {

        @Override
        public V value() {
            return valueFunction.apply(o);
        }

        @Override
        public boolean isValid() {
            V v = valueFunction.apply(o);
            return v != null && v.isValid();
        }

        public NamedValueConstraint<O, V> withName(String name) {
            return new NamedValueConstraint<>(o, valueFunction, name);
        }
    }
}