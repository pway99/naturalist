package com.naturalist.ddd;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Abstract base for all behavioral collections in the domain.
 * <p>
 * Any public-facing method that returns multiple results returns a concrete subtype of
 * {@code BehavioralCollection} rather than a raw {@code List<T>}, {@code Set<T>}, or
 * {@code Collection<T>}. This centralises domain query vocabulary — filtering, grouping,
 * aggregation — in one type per result concept rather than scattering stream chains
 * across call sites.
 * <p>
 * The type bound {@code T extends Observable} is not incidental. Every domain type —
 * {@link NamedEntity}, {@link Aggregate}, {@link ValueObject} — implements
 * {@link Observable}, so a {@code BehavioralCollection} can hold any of them while
 * retaining access to {@code invariants()} on each member.
 * <p>
 * <b>Immutability.</b> The backing list is sealed by {@link List#copyOf} in this
 * constructor and nowhere else. Every code path that produces a concrete collection —
 * {@code of(...)}, {@code empty()}, and all {@code with*} transformation methods on
 * subclasses — flows through {@code super(...)}, so the invariant is structural, not
 * documented and hoped for.
 * <p>
 * <b>Constructor visibility.</b> This constructor is {@code protected} rather than
 * package-private because concrete subclasses live in {@code <domain>-api} modules that
 * are separate packages from {@code kernels/framework}. {@code protected} is the minimum
 * visibility that permits {@code super(...)} calls across that package boundary. The
 * concrete subclass constructor is package-private, preserving instantiation control at
 * the domain level (ADR-012).
 * <p>
 * <b>Content access.</b> Consumers use {@link #stream()} as the primary entry point for
 * further composition. Subclass {@code with*} transformation methods filter via
 * {@code stream().filter(...).toList()} and pass the result to {@code super(...)},
 * keeping the defensive copy structural and the implementation standard library only.
 *
 * @param <T> the domain type held by this collection; must implement {@link Observable}
 * @see com.naturalist.observability.Observable
 */
public abstract class BehavioralCollection<T extends Observable> implements Observable {

    private final List<T> elements;

    protected BehavioralCollection(Collection<T> elements) {
        this.elements = List.copyOf(elements);
    }

    /**
     * Primary content-access point. Consumers compose further operations via {@link Stream}.
     */
    public Stream<T> stream() {
        return elements.stream();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public int size() {
        return elements.size();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.notNull(this, c -> ((BehavioralCollection<?>) c).elements, "elements");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BehavioralCollection<?> that = (BehavioralCollection<?>) o;
        return Objects.equals(elements, that.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(elements);
    }
}
