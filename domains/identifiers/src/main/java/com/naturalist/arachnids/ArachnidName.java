package com.naturalist.arachnids;

import com.naturalist.ddd.EntityName;

public final class ArachnidName extends EntityName {
    private ArachnidName(String value) {
        super(value);
    }

    public static ArachnidName of(String value) {
        return new ArachnidName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
