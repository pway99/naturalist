package com.naturalist.data;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.HashMap;
import java.util.Map;

public class NaturalistDatabase implements BeforeEachCallback {
    final Map<Class<? extends TestEntitySource<?,?,?>>, TestEntitySource<?, ?,?>> entitySourceMap = new HashMap<>();

    public static NaturalistDatabase create() {
        return new NaturalistDatabase();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        entitySourceMap.clear();
    }

    @SuppressWarnings("unchecked")
    public <ETS extends TestEntitySource<?,?,?>> ETS get(Class<? extends TestEntitySource<?,?,?>> testEntitySourceClass) {
        ETS ets = (ETS) entitySourceMap.get(testEntitySourceClass);
        if (ets == null) {
            try {
                ets = (ETS)testEntitySourceClass.getDeclaredConstructor().newInstance();
                entitySourceMap.put(testEntitySourceClass, ets);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return ets;
    }
}
