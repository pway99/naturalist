package com.naturalist.plants.cultivar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class CultivarId extends PersistenceId<Long> {

    private CultivarId(Long value) {
        super(value);
    }

    @JsonCreator
    public static CultivarId of(Long value) {
        return new CultivarId(value);
    }
}
