package com.naturalist.plants.heritage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class SeedLineageId extends PersistenceId<Long> {

    private SeedLineageId(Long value) {
        super(value);
    }

    @JsonCreator
    public static SeedLineageId of(Long value) {
        return new SeedLineageId(value);
    }
}
