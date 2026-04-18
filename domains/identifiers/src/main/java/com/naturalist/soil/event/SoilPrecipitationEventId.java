package com.naturalist.soil.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class SoilPrecipitationEventId extends PersistenceId<Long> {

    private SoilPrecipitationEventId(Long value) {
        super(value);
    }

    @JsonCreator
    public static SoilPrecipitationEventId of(Long value) {
        return new SoilPrecipitationEventId(value);
    }
}
