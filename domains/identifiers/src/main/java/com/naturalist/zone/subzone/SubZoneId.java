package com.naturalist.zone.subzone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class SubZoneId extends PersistenceId<Long> {

    private SubZoneId(Long value) {
        super(value);
    }

    @JsonCreator
    public static SubZoneId of(Long value) {
        return new SubZoneId(value);
    }
}
