package com.naturalist.data;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.HashMap;
import java.util.Map;

public class NaturalistDatabase implements BeforeEachCallback {
    final Map<Class<?>, Object> sourceMap = new HashMap<>();

    public static NaturalistDatabase create() {
        return new NaturalistDatabase();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        sourceMap.clear();
    }

    @SuppressWarnings("unchecked")
    public <ETS extends TestEntitySource<?,?,?>> ETS get(Class<? extends TestEntitySource<?,?,?>> testEntitySourceClass) {
        ETS ets = (ETS) sourceMap.get(testEntitySourceClass);
        if (ets == null) {
            try {
                ets = (ETS) testEntitySourceClass.getDeclaredConstructor().newInstance();
                sourceMap.put(testEntitySourceClass, ets);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return ets;
    }

    @SuppressWarnings("unchecked")
    public <NTS extends NamedTestEntitySource<?,?>> NTS getNamed(Class<? extends NamedTestEntitySource<?,?>> namedTestEntitySourceClass) {
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
