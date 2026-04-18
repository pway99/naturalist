package com.naturalist.arachnids;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class ArachnidId extends PersistenceId<Long> {
    private ArachnidId(Long value) { super(value); }

    @JsonCreator
    public static ArachnidId of(Long value) { return new ArachnidId(value); }
}
