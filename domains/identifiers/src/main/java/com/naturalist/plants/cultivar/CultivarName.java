package com.naturalist.plants.cultivar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

public final class CultivarName extends EntityName {

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
