package com.naturalist.soil;

import com.naturalist.ddd.EntityName;

public final class SoilProfileName extends EntityName {

    private SoilProfileName(String value) {
        super(value);
    }

    public static SoilProfileName of(String value) {
        return new SoilProfileName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
