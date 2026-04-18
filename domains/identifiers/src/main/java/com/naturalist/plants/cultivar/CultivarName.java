package com.naturalist.plants.cultivar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.CatalogName;

public final class CultivarName extends CatalogName {

    private CultivarName(String value) {
        super(value);
    }

    @JsonCreator
    public static CultivarName of(String value) {
        return new CultivarName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
