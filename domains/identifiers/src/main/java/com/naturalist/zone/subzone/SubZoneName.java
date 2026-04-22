package com.naturalist.zone.subzone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

public final class SubZoneName extends EntityName {

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