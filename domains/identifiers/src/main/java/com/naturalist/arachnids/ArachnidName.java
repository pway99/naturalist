package com.naturalist.arachnids;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.CatalogName;

public final class ArachnidName extends CatalogName {
    private ArachnidName(String value) { super(value); }

    @JsonCreator
    public static ArachnidName of(String value) { return new ArachnidName(value); }

    @Override
    protected int maxLength() {
        return 64;
    }
}
