package com.naturalist.chemistry.compound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class CompoundId extends PersistenceId<Long> {

    private CompoundId(Long value) {
        super(value);
    }

    @JsonCreator
    public static CompoundId of(Long value) {
        return new CompoundId(value);
    }
}
