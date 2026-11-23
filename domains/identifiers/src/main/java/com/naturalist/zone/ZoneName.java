package com.naturalist.zone;

import com.naturalist.ddd.EntityName;

public final class ZoneName extends EntityName {

    private ZoneName(String value) {
        super(value);
    }

    public static ZoneName of(String value) {
        return new ZoneName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}