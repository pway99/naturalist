package com.naturalist.observability.constraints;

import com.naturalist.ddd.PersistenceId;
import com.naturalist.observability.Constraint;

import java.util.Collection;
import java.util.function.Function;

public interface PersistenceIdConstraints {

    record PersistenceIdCollectionConstraint<E extends PersistenceId<?>>(
            Collection<E> value,
            String name
    ) implements Constraint<Collection<E>> {

        @Override
        public boolean isValid() {
            if (value == null) {
                return false;
            }
            for (E e : value) {
                if (e == null || e.isNotValid()) {
                    return false;
                }
            }
            return true;
        }

        public PersistenceIdCollectionConstraint<E> withName(String name) {
            return new PersistenceIdCollectionConstraint<>(value, name);
        }
    }

    /**
     * Null means "not yet assigned" — valid pre-insert state per ADR-005.
     * Only fails for a non-null id whose wrapped value is malformed.
     */
    record PersistenceIdConstraint<E extends PersistenceId<?>>(
            E value,
            String name
    ) implements Constraint<E> {

        @Override
        public boolean isValid() {
            return value == null || value.isValid();
        }

        public PersistenceIdConstraint<E> withName(String name) {
            return new PersistenceIdConstraint<>(value, name);
        }
    }

    /**
     * Null means "not yet assigned" — valid pre-insert state per ADR-005.
     * Only fails for a non-null id whose wrapped value is malformed.
     */
    record PersistenceIdByFunctionConstraint<O, E extends PersistenceId<?>>(
            O o,
            Function<O, E> valueFunction,
            String name
    ) implements Constraint<E> {

        @Override
        public E value() {
            return valueFunction.apply(o);
        }

        @Override
        public boolean isValid() {
            E e = valueFunction.apply(o);
            return e == null || e.isValid();
        }

        public PersistenceIdByFunctionConstraint<O, E> withName(String name) {
            return new PersistenceIdByFunctionConstraint<>(o, valueFunction, name);
        }
    }
}