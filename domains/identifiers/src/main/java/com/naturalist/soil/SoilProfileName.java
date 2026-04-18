package com.naturalist.soil;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.CatalogName;

public final class SoilProfileName extends CatalogName {

    private SoilProfileName(String value) {
        super(value);
    }

    @JsonCreator
    public static SoilProfileName of(String value) {
        return new SoilProfileName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
