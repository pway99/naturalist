package com.naturalist.naturalist;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class NaturalistId extends PersistenceId<Long> {

    private NaturalistId(Long value) {
        super(value);
    }

    @JsonCreator
    public static NaturalistId of(Long value) {
        return new NaturalistId(value);
    }
}
