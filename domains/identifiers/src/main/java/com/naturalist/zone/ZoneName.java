package com.naturalist.zone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.CatalogName;

public final class ZoneName extends CatalogName {

    private ZoneName(String value) {
        super(value);
    }

    @JsonCreator
    public static ZoneName of(String value) {
        return new ZoneName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}