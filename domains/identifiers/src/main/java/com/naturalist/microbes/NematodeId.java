package com.naturalist.microbes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class NematodeId extends PersistenceId<Long> {
    private NematodeId(Long value) { super(value); }

    @JsonCreator
    public static NematodeId of(Long value) { return new NematodeId(value); }
}
