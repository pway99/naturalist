package com.naturalist.soil.event;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class IrrigationEventId extends EntityId {

    private IrrigationEventId(UUID value) {
        super(value);
    }

    public static IrrigationEventId of(UUID value) {
        return new IrrigationEventId(value);
    }

    public static IrrigationEventId create() {
        return new IrrigationEventId(EntityId.newUUID());
    }
}
