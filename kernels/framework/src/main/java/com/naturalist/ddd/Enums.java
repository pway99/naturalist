package com.naturalist.ddd;

import java.util.HashMap;
import java.util.Map;

/**
 * Lookups of enum constants by name that never throw, for values read from storage or a request.
 * {@link Enum#valueOf} throws on an unknown or null name with nothing counted; a lookup here hands
 * on null instead, and the caller's observation ({@code enumValue} on the source, C1) counts it.
 *
 * <p>A hot path: every enum column of every row read goes through {@link #named}. Each enum type's
 * name-to-constant map is built once, on first use, and held in a {@link ClassValue}, so a lookup is
 * one map read with no allocation. {@code type} is always a class literal at the call site, so it is
 * not observed (J7, framework hot-path exemption).
 */
public final class Enums {

    private static final ClassValue<Map<String, Enum<?>>> BY_NAME = new ClassValue<>() {
        @Override
        protected Map<String, Enum<?>> computeValue(Class<?> type) {
            Map<String, Enum<?>> byName = new HashMap<>();
            for (Object constant : type.getEnumConstants()) {
                Enum<?> e = (Enum<?>) constant;
                byName.put(e.name(), e);
            }
            return Map.copyOf(byName);
        }
    };

    private Enums() {
    }

    /** The constant of {@code type} whose {@link Enum#name()} is exactly {@code name}, or null when none is. */
    public static <E extends Enum<E>> E named(Class<E> type, String name) {
        if (name == null) return null;
        return type.cast(BY_NAME.get(type).get(name));
    }
}
