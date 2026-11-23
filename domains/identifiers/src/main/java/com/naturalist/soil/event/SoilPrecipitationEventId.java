package com.naturalist.soil.event;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class SoilPrecipitationEventId extends EntityId {

    private SoilPrecipitationEventId(UUID value) {
        super(value);
    }

    public static SoilPrecipitationEventId of(UUID value) {
        return new SoilPrecipitationEventId(value);
    }

    public static SoilPrecipitationEventId create() {
        return new SoilPrecipitationEventId(EntityId.newUUID());
    }
}
