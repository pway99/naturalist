package com.naturalist.observability;

import com.naturalist.ddd.*;
import com.naturalist.observability.constraints.*;

import java.util.*;
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

    /**
     * Asserts that the extracted string matches the project's standard kebab-case
     * slug format — the same rule {@link com.naturalist.ddd.EntityName} subclasses
     * apply to their own {@code value} string. Use for value objects that wrap a
     * kebab slug (a {@code DomainId}, a future {@code Tag}, etc.) so the format
     * rule is declared once instead of re-derived per type.
     *
     * <p>Null and empty values fail. Pair with {@link #notBlank} when the diagnostic
     * message should distinguish &ldquo;missing&rdquo; from &ldquo;malformed&rdquo;.
     */
    public Constraints kebabFormat(String value, String name) {
        return kebabFormat(value, Function.identity(), name);
    }

    public <T> Constraints kebabFormat(T t, Function<T, String> valueFunction, String name) {
        return add(new KebabFormatConstraint<>(t, valueFunction, name));
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

    /**
     * Asserts the value lies within {@code [min, max]} inclusive. Null values
     * pass (pair with {@link #notNull} when presence is also required); either
     * bound may be null to express a one-sided range — see {@link #atLeast}
     * and {@link #atMost} for the conventional spellings.
     */
    public <V extends Comparable<V>> Constraints inRange(V value, V min, V max, String name) {
        return inRange(value, Function.identity(), min, max, name);
    }

    public <T, V extends Comparable<V>> Constraints inRange(T t, Function<T, V> valueFunction, V min, V max, String name) {
        return add(new InRangeConstraint<>(t, valueFunction, min, max, name));
    }

    /**
     * Asserts the value is greater than or equal to {@code min}. Null values
     * pass (pair with {@link #notNull} when presence is also required).
     */
    public <V extends Comparable<V>> Constraints atLeast(V value, V min, String name) {
        return inRange(value, Function.identity(), min, null, name);
    }

    public <T, V extends Comparable<V>> Constraints atLeast(T t, Function<T, V> valueFunction, V min, String name) {
        return inRange(t, valueFunction, min, null, name);
    }

    /**
     * Asserts the value is less than or equal to {@code max}. Null values pass
     * (pair with {@link #notNull} when presence is also required).
     */
    public <V extends Comparable<V>> Constraints atMost(V value, V max, String name) {
        return inRange(value, Function.identity(), null, max, name);
    }

    public <T, V extends Comparable<V>> Constraints atMost(T t, Function<T, V> valueFunction, V max, String name) {
        return inRange(t, valueFunction, null, max, name);
    }

    public List<Constraint<?>> collected() {
        return Collections.unmodifiableList(constraints);
    }

    private Constraints add(Constraint<?> constraint) {
        constraints.add(constraint);
        return this;
    }
}
