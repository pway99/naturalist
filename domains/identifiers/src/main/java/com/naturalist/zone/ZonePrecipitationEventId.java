package com.naturalist.zone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class ZonePrecipitationEventId extends EntityId {

    private ZonePrecipitationEventId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static ZonePrecipitationEventId of(UUID value) {
        return new ZonePrecipitationEventId(value);
    }

    public static ZonePrecipitationEventId create() {
        return new ZonePrecipitationEventId(EntityId.newUUID());
    }
}
