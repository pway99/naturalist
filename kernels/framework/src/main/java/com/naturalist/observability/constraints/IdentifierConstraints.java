package com.naturalist.observability.constraints;

import com.naturalist.ddd.EntityId;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Constraint;

import java.util.Set;

/**
 * Polymorphic identifier constraint — dispatches validation based on the runtime
 * type of the supplied value. Accepts either an {@link EntityName} (slug identity)
 * or an {@link EntityId} (UUIDv7 surrogate identity) and routes to the appropriate
 * validator.
 *
 * <p>Used at boundaries where the identifier branch is not fixed at compile time.
 * The canonical case is the {@code NAME} generic on
 * {@link com.naturalist.data.AbstractEntityQuery} and
 * {@link com.naturalist.data.AbstractEntityRepository}, which binds to either
 * {@link EntityName} or {@link EntityId} depending on the domain.
 */
public interface IdentifierConstraints {

    record IdentifierConstraint<V>(
            V value,
            String name
    ) implements Constraint<V> {

        @Override
        public boolean isValid() {
            return isValidIdentifier(value);
        }

        @Override
        public IdentifierConstraint<V> withName(String name) {
            return new IdentifierConstraint<>(value, name);
        }

        @Override
        public String errorMessage() {
            return errorMessageFor(value);
        }
    }

    record IdentifierSetConstraint<V>(
            Set<V> value,
            String name
    ) implements Constraint<Set<V>> {

        @Override
        public boolean isValid() {
            if (value == null) {
                return false;
            }
            for (V v : value) {
                if (!isValidIdentifier(v)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public IdentifierSetConstraint<V> withName(String name) {
            return new IdentifierSetConstraint<>(value, name);
        }
    }

    static boolean isValidIdentifier(Object value) {
        return switch (value) {
            case null -> false;
            case EntityName en -> en.isValid();
            case EntityId eid -> eid.isValid();
            default -> false;
        };
    }

    static String errorMessageFor(Object value) {
        return switch (value) {
            case null -> "null value";
            case EntityName en -> en.errorMessage();
            case EntityId eid -> eid.errorMessage();
            default -> "unsupported identifier type: " + value.getClass().getName();
        };
    }
}
