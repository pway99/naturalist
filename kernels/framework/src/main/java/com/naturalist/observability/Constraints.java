package com.naturalist.observability;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.ddd.NamedValue;
import com.naturalist.ddd.PersistenceId;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.constraints.NamedValueConstraints;
import com.naturalist.observability.constraints.ObservableConstraint;
import com.naturalist.observability.constraints.PersistenceIdConstraints;
import com.naturalist.observability.constraints.EntityNameConstraints;
import com.naturalist.observability.constraints.NotBlankConstraint;
import com.naturalist.observability.constraints.NotNullConstraint;
import com.naturalist.observability.constraints.ValueObjectOrNullConstraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;


public class Constraints {

    final List<Constraint<?>> constraints = new ArrayList<>();

    public <E extends Entity<?, ?>> Constraints entity(E entity, String name) {
        return entity(entity, Function.identity(), name);
    }

    public <O, E extends Entity<?, ?>> Constraints entity(O o, Function<O, E> valueFunction, String name) {
        return add(new ObservableConstraint<>(o, valueFunction, name));
    }

    public <E extends NamedEntity<?>> Constraints namedEntity(E entity, String name) {
        return namedEntity(entity, Function.identity(), name);
    }

    public <O, E extends NamedEntity<?>> Constraints namedEntity(O o, Function<O, E> valueFunction, String name) {
        return add(new ObservableConstraint<>(o, valueFunction, name));
    }

    public <V extends ValueObject> Constraints valueObject(V valueObject, String name) {
        return valueObject(valueObject, Function.identity(), name);
    }

    public <O, V extends ValueObject> Constraints valueObject(O o, Function<O, V> valueFunction, String name) {
        return add(new ObservableConstraint<>(o, valueFunction, name));
    }

    public <O, V extends ValueObject> Constraints valueObjectOrNull(O o, Function<O, V> valueFunction, String name) {
        return add(new ValueObjectOrNullConstraint<>(o, valueFunction, name));
    }

    /**
     * Validate any {@link Observable} child and descend into its invariants. Use for
     * Observable members that are neither {@link com.naturalist.ddd.Entity} nor
     * {@link ValueObject} — most commonly a
     * {@link com.naturalist.ddd.BehavioralCollection} held by an
     * {@link com.naturalist.ddd.Aggregate}.
     */
    public <O, V extends Observable> Constraints observable(O o, Function<O, V> valueFunction, String name) {
        return add(new ObservableConstraint<>(o, valueFunction, name));
    }

    public Constraints entityId(PersistenceId<?> entityId, String name) {
        return add(new PersistenceIdConstraints.PersistenceIdConstraint<>(entityId, name));
    }

    public Constraints entityIdCollection(Collection<PersistenceId<?>> entityIds, String name) {
        return add(new PersistenceIdConstraints.PersistenceIdCollectionConstraint<>(entityIds, name));
    }

    public <O, E extends PersistenceId<?>> Constraints entityId(O o, Function<O, E> valueFunction, String name) {
        return add(new PersistenceIdConstraints.PersistenceIdByFunctionConstraint<>(o, valueFunction, name));
    }

    public <E extends EntityName> Constraints entityName(E e, String name) {
        return add(new EntityNameConstraints.EntityNameConstraint<>(e, name));
    }

    public <E extends EntityName> Constraints entityNameCollection(Collection<E> entityNames, String name) {
        return add(new EntityNameConstraints.EntityNameCollectionConstraint<>(entityNames, name));
    }

    public <O, V extends NamedValue<?>> Constraints namedValue(O o, Function<O, V> valueFunction, String name) {
        return add(new NamedValueConstraints.NamedValueConstraint<>(o, valueFunction, name));
    }

    public Constraints notBlank(String value, String name) {
        return notBlank(value, Function.identity(), name);
    }

    public <T> Constraints notBlank(T t, Function<T, String> valueFunction, String name) {
        return add(new NotBlankConstraint<>(t, valueFunction, name));
    }

    public <R> Constraints notNull(R value, String name) {
        return notNull(value, Function.identity(), name);
    }

    public <T, R> Constraints notNull(T t, Function<T, R> valueFunction, String name) {
        return add(new NotNullConstraint<>(t, valueFunction, name));
    }

    public List<Constraint<?>> collected() {
        return Collections.unmodifiableList(constraints);
    }

    private Constraints add(Constraint<?> constraint) {
        constraints.add(constraint);
        return this;
    }
}