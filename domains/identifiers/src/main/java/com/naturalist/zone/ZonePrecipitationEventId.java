package com.naturalist.zone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class ZonePrecipitationEventId extends PersistenceId<Long> {

    private ZonePrecipitationEventId(Long value) {
        super(value);
    }

    @JsonCreator
    public static ZonePrecipitationEventId of(Long value) {
        return new ZonePrecipitationEventId(value);
    }
}
