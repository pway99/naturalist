package com.naturalist.ddd;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observable;
import com.naturalist.observability.Observer;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract base for keyed domain collections. Extends {@link BehavioralCollection}
 * with a secondary index that maps each element to a key, enabling grouped lookup
 * without raw {@code Map} types at the port boundary.
 *
 * <p>Two construction modes:
 * <ul>
 *   <li><b>Element-derived grouping</b> — a key extractor groups elements automatically.
 *       Use when the grouping key is a field on the element itself.</li>
 *   <li><b>Pre-computed grouping</b> — the caller supplies the mapping. Use when the
 *       grouping key is derived externally (e.g. hierarchical ancestor lookup).</li>
 * </ul>
 *
 * <p>{@link #elementsForKey(Object)} is {@code protected} — domain subclasses wrap it
 * in a typed method returning their own {@link BehavioralCollection} subclass.
 *
 * @param <K> the key type for the index
 * @param <V> the element type; must implement {@link Observable}
 */
public abstract class BehavioralMap<K, V extends Observable> extends BehavioralCollection<V> {

    private final Map<K, List<V>> index;
    private final Observer observer;

    /**
     * Element-derived grouping. Each element is assigned to a key via {@code keyExtractor}.
     */
    protected BehavioralMap(Collection<V> elements, Function<V, K> keyExtractor) {
        super(elements);
        this.observer = Observer.forClass(getClass());
        this.index = elements.stream()
                .collect(Collectors.groupingBy(keyExtractor,
                        Collectors.toUnmodifiableList()));
    }

    /**
     * Pre-computed grouping. The caller supplies the key-to-elements mapping; the flat
     * list is derived by flattening all values.
     */
    protected BehavioralMap(Map<K, ? extends Collection<V>> groups) {
        super(groups.values().stream().flatMap(Collection::stream).toList());
        this.observer = Observer.forClass(getClass());
        this.index = groups.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> List.copyOf(e.getValue())));
    }

    /**
     * Returns the elements grouped under {@code key}, or an empty list if the key is
     * absent. Observes the argument — a null key emits a warning metric via
     * {@link Observer#arguments} with {@code .observe()} (no throw) and returns empty.
     */
    protected List<V> elementsForKey(K key) {
        observer.arguments("elementsForKey", i -> i.notNull(key, "key"))
                .observe(Level.WARN);
        if (key == null) {
            return List.of();
        }
        return index.getOrDefault(key, List.of());
    }

    public boolean hasKey(K key) {
        return key != null && index.containsKey(key);
    }

    public Set<K> keys() {
        return Collections.unmodifiableSet(index.keySet());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.notNull(this, c -> ((BehavioralMap<?, ?>) c).index, "index");
    }
}
