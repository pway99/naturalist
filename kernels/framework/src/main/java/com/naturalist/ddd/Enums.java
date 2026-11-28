package com.naturalist.ddd;

import java.util.Arrays;

/**
 * Lookups of enum constants by name that never throw, for values read from storage or a request.
 * {@link Enum#valueOf} throws on an unknown or null name with nothing counted; a lookup here hands
 * on null instead, and the caller's observation ({@code enumValue} on the source, C1) counts it.
 */
public final class Enums {

    private Enums() {
    }

    /** The constant of {@code type} whose {@link Enum#name()} is exactly {@code name}, or null when none is. */
    public static <E extends Enum<E>> E named(Class<E> type, String name) {
        if (name == null) return null;
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.name().equals(name))
                .findFirst()
                .orElse(null);
    }
}
