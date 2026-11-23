package com.naturalist.zone.subzone;

import com.naturalist.ddd.EntityName;

public final class SubZoneName extends EntityName {

    private SubZoneName(String value) {
        super(value);
    }

    public static SubZoneName of(String value) {
        return new SubZoneName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}