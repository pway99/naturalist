package com.naturalist.zone.subzone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.CatalogName;

public final class SubZoneName extends CatalogName {

    private SubZoneName(String value) {
        super(value);
    }

    @JsonCreator
    public static SubZoneName of(String value) {
        return new SubZoneName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}