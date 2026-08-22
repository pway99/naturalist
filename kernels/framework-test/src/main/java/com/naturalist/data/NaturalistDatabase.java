package com.naturalist.data;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory registry of {@link TestEntitySource} instances keyed by class. Plays
 * the role of the in-memory database shared across repository adapters during
 * development and testing.
 *
 * <p>This class holds no JUnit lifecycle concern — it is a plain source registry
 * usable from any context (main-wired controllers during pre-RDBMS development,
 * tests, CLI tools). For the JUnit per-method reset behavior, use
 * {@link NaturalistTestExtension}.
 */
public class NaturalistDatabase {
    final Map<Class<?>, Object> sourceMap = new HashMap<>();

    protected NaturalistDatabase() {
    }

    public static NaturalistDatabase create() {
        return new NaturalistDatabase();
    }

    public void clear() {
        sourceMap.clear();
    }

    public <NTS extends TestEntitySource<?, ?>> NTS getNamed(Class<NTS> sourceClass) {
        @SuppressWarnings("unchecked")
        NTS cached = (NTS) sourceMap.get(sourceClass);
        if (cached != null) {
            return cached;
        }
        try {
            NTS nts = sourceClass
                    .getDeclaredConstructor(NaturalistDatabase.class)
                    .newInstance(this);
            sourceMap.put(sourceClass, nts);
            return nts;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
