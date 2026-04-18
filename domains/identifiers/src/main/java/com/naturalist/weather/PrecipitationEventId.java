package com.naturalist.weather;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class PrecipitationEventId extends PersistenceId<Long> {

    private PrecipitationEventId(Long value) {
        super(value);
    }

    @JsonCreator
    public static PrecipitationEventId of(Long value) {
        return new PrecipitationEventId(value);
    }
}
