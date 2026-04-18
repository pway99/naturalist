package com.naturalist.worms;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class WormId extends PersistenceId<Long> {
    private WormId(Long value) { super(value); }

    @JsonCreator
    public static WormId of(Long value) { return new WormId(value); }
}
