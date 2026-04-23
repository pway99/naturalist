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
 * {@link NaturalistDatabaseExtension}.
 */
public class NaturalistDatabase {
    final Map<Class<?>, Object> sourceMap = new HashMap<>();

    protected NaturalistDatabase() {}

    public static NaturalistDatabase create() {
        return new NaturalistDatabase();
    }

    public void clear() {
        sourceMap.clear();
    }

    @SuppressWarnings("unchecked")
    public <NTS extends TestEntitySource<?,?>> NTS getNamed(Class<? extends TestEntitySource<?,?>> namedTestEntitySourceClass) {
        NTS nts = (NTS) sourceMap.get(namedTestEntitySourceClass);
        if (nts == null) {
            try {
                nts = (NTS) namedTestEntitySourceClass.getDeclaredConstructor().newInstance();
                sourceMap.put(namedTestEntitySourceClass, nts);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return nts;
    }
}
