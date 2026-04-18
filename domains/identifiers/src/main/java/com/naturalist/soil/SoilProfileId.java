package com.naturalist.soil;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class SoilProfileId extends PersistenceId<Long> {

    private SoilProfileId(Long value) {
        super(value);
    }

    @JsonCreator
    public static SoilProfileId of(Long value) {
        return new SoilProfileId(value);
    }
}
