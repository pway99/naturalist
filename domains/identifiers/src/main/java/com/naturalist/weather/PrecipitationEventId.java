package com.naturalist.weather;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class PrecipitationEventId extends EntityId {

    private PrecipitationEventId(UUID value) {
        super(value);
    }

    public static PrecipitationEventId of(UUID value) {
        return new PrecipitationEventId(value);
    }

    public static PrecipitationEventId create() {
        return new PrecipitationEventId(EntityId.newUUID());
    }
}
