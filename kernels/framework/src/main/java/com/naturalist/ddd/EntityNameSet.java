package com.naturalist.ddd;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observable;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Immutable set of {@link EntityName}s — the names-only analogue of
 * {@link BehavioralCollection}. Used as an argument type wherever a set of
 * names enters the domain (e.g. a future
 * {@code findByNameSet(EntityNameSet<...>)}). The unbounded "all-names"
 * read shape is gone: bulk reads paginate via the kernel's
 * {@code findPage(PageRequest)} contract.
 *
 * <p><b>Why concrete, not abstract.</b> Unlike {@link BehavioralCollection}, which
 * anchors domain-specific filtering vocabulary per entity type, a name set carries no
 * per-domain behaviour — a name is just a name. The generic parameter
 * ({@code EntityNameSet<InsectSpeciesName>} vs {@code EntityNameSet<ElementName>})
 * is the type-system safety boundary; subclassing would add code without adding meaning.
 *
 * <p><b>Immutability.</b> The backing {@link Set} is sealed by {@link Set#copyOf} in
 * the private constructor. Every public factory flows through it. There are no mutators;
 * any "change" is a new instance.
 *
 * <p><b>Observability.</b> The set validates itself as a single constraint
 * ({@link Constraints#entityNameSet(EntityNameSet, String)}): the reference is non-null
 * and every contained name is non-null and valid.
 *
 * @param <NAME> the concrete {@link EntityName} subtype held by this set
 */
public final class EntityNameSet<NAME extends EntityName> implements Observable {

    private final Set<NAME> names;

    private EntityNameSet(Collection<NAME> names) {
        this.names = Set.copyOf(names);
    }

    public static <NAME extends EntityName> EntityNameSet<NAME> of(Collection<NAME> names) {
        return new EntityNameSet<>(names);
    }

    public static <NAME extends EntityName> EntityNameSet<NAME> empty() {
        return new EntityNameSet<>(Set.of());
    }

    public Stream<NAME> stream() {
        return names.stream();
    }

    public boolean isEmpty() {
        return names.isEmpty();
    }

    public int size() {
        return names.size();
    }

    public boolean contains(NAME name) {
        return names.contains(name);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.entityNameSet(this, "names");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityNameSet<?> that = (EntityNameSet<?>) o;
        return Objects.equals(names, that.names);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(names);
    }
}
