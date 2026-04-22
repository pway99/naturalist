package com.naturalist.microbes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

public final class NematodeName extends EntityName {
    private NematodeName(String value) { super(value); }

    @JsonCreator
    public static NematodeName of(String value) { return new NematodeName(value); }

    @Override
    protected int maxLength() {
        return 64;
    }
}
