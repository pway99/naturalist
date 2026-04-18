package com.naturalist.soil.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class TillageEventId extends PersistenceId<Long> {

    private TillageEventId(Long value) {
        super(value);
    }

    @JsonCreator
    public static TillageEventId of(Long value) {
        return new TillageEventId(value);
    }
}
