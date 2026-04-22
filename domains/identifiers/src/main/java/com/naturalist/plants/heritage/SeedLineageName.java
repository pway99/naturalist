package com.naturalist.plants.heritage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

public final class SeedLineageName extends EntityName {

    private SeedLineageName(String value) {
        super(value);
    }

    @JsonCreator
    public static SeedLineageName of(String value) {
        return new SeedLineageName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
