package com.naturalist.zone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class ZoneId extends PersistenceId<Long> {

    private ZoneId(Long value) {
        super(value);
    }

    @JsonCreator
    public static ZoneId of(Long value) {
        return new ZoneId(value);
    }
}
