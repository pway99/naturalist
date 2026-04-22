package com.naturalist.weather;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class PrecipitationEventId extends EntityId {

    private PrecipitationEventId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static PrecipitationEventId of(UUID value) {
        return new PrecipitationEventId(value);
    }
}
