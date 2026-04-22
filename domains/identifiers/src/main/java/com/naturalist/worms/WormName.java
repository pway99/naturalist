package com.naturalist.worms;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

public final class WormName extends EntityName {
    private WormName(String value) { super(value); }

    @JsonCreator
    public static WormName of(String value) { return new WormName(value); }

    @Override
    protected int maxLength() {
        return 64;
    }
}
