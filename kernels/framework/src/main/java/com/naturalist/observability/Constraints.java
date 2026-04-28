package com.naturalist.observability;

import com.naturalist.ddd.*;
import com.naturalist.observability.constraints.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


public class Constraints {

    final List<Constraint<?>> constraints = new ArrayList<>();

    public <E extends Named<?>> Constraints namedEntity(E entity, String name) {
        return namedEntity(entity, Function.identity(), name);
    }

    public <O, E extends Named<?>> Constraints namedEntity(O o, Function<O, E> valueFunction, String name) {
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
     * Validate a non-null {@link Collection} of {@link ValueObject} elements and
     * descend into each element's own {@link ValueObject#invariants()}. Null
     * collection fails; empty collection passes — pair with {@link #notEmpty}
     * when empty is also illegal.
     */
    public <O, V extends ValueObject> Constraints valueObjectCollection(O o, Function<O, Collection<V>> valueFunction, String name) {
        return add(new ValueObjectCollectionConstraint<>(o, valueFunction, name));
    }

    /**
     * Validate any {@link Observable} child and descend into its invariants. Use for
     * Observable members that are neither {@link Entity} nor {@link ValueObject} —
     * most commonly a {@link com.naturalist.ddd.BehavioralCollection} held by an
     * {@link com.naturalist.ddd.Aggregate}.
     */
    public <O, V extends Observable> Constraints observable(O o, Function<O, V> valueFunction, String name) {
        return add(new ObservableConstraint<>(o, valueFunction, name));
    }

    public <E extends EntityName> Constraints entityName(E e, String name) {
        return add(new EntityNameConstraints.EntityNameConstraint<>(e, name));
    }

    /**
     * Null-tolerant variant of {@link #entityName}. A null value passes; a
     * non-null value must satisfy {@link EntityName#isValid()} (kebab-case
     * format and subtype {@code maxLength()}). Use for {@code @Nullable
     * EntityName} record components — the reference is optional, but when
     * present it must still be a valid slug.
     */
    public <E extends EntityName> Constraints entityNameOrNull(E e, String name) {
        return add(new EntityNameConstraints.EntityNameOrNullConstraint<>(e, name));
    }

    public <F extends EntityId> Constraints entityId(F f, String name) {
        return add(new EntityIdConstraints.EntityIdConstraint<>(f, name));
    }

    /**
     * Polymorphic identifier check — dispatches at runtime to
     * {@link #entityName}- or {@link #entityId}-equivalent validation based on the
     * concrete type of {@code value}. Use at boundaries where the identifier branch
     * is not fixed at compile time (for example, the {@code NAME} generic on
     * {@link com.naturalist.data.AbstractEntityQuery}).
     */
    public <V> Constraints identifier(V value, String name) {
        return add(new IdentifierConstraints.IdentifierConstraint<>(value, name));
    }

    /**
     * Polymorphic identifier-set check — null set is invalid; every element is
     * validated by runtime type (see {@link #identifier}).
     */
    public <V> Constraints identifierSet(Set<V> value, String name) {
        return add(new IdentifierConstraints.IdentifierSetConstraint<>(value, name));
    }

    public <E extends EntityName> Constraints entityNameCollection(Collection<E> entityNames, String name) {
        return add(new EntityNameConstraints.EntityNameCollectionConstraint<>(entityNames, name));
    }

    public <E extends EntityName> Constraints entityNameSet(EntityNameSet<E> set, String name) {
        return add(new EntityNameConstraints.EntityNameSetConstraint<>(set, name));
    }

    public <O, E extends EntityName> Constraints entityNameSet(O o, Function<O, EntityNameSet<E>> valueFunction, String name) {
        return add(new EntityNameConstraints.EntityNameSetConstraint<>(o == null ? null : valueFunction.apply(o), name));
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

    /**
     * Rejects null, empty {@link Collection}, empty {@link java.util.Map}, and
     * empty {@link CharSequence}. Non-collection, non-null values pass. Pair with
     * a descent constraint (e.g. {@link #valueObjectCollection}) to assert both
     * "has elements" and "each element is valid".
     */
    public <T, R> Constraints notEmpty(T t, Function<T, R> valueFunction, String name) {
        return add(new NotEmptyConstraint<>(t, valueFunction, name));
    }

    public List<Constraint<?>> collected() {
        return Collections.unmodifiableList(constraints);
    }

    private Constraints add(Constraint<?> constraint) {
        constraints.add(constraint);
        return this;
    }
}
