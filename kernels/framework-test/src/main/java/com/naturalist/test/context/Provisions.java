package com.naturalist.test.context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * What one {@link TestContext} builds by hand. A factory receives the graph as seen from its context, so
 * what it asks for resolves under that context's own provisions first.
 */
public final class Provisions {
    private final Map<Class<?>, Function<TestGraph, ?>> factories = new LinkedHashMap<>();

    Provisions() {
    }

    /** Builds {@code type} with {@code factory}, once per graph, instead of by reflection. */
    public <T> void provide(Class<T> type, Function<TestGraph, T> factory) {
        if (type == null || factory == null) {
            throw new TestGraphException("provide needs a type and a factory");
        }
        if (factories.putIfAbsent(type, factory) != null) {
            throw new TestGraphException(type.getName() + " is provided twice");
        }
    }

    /** The factory for {@code type}, or null when this context leaves it to reflection. */
    Function<TestGraph, ?> factory(Class<?> type) {
        return factories.get(type);
    }
}
