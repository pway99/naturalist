package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class PlantId extends PersistenceId<Long> {

    private PlantId(Long value) {
        super(value);
    }

    @JsonCreator
    public static PlantId of(Long value) {
        return new PlantId(value);
    }
}
