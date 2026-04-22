package com.naturalist.soil.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class IrrigationEventId extends EntityId {

    private IrrigationEventId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static IrrigationEventId of(UUID value) {
        return new IrrigationEventId(value);
    }
}
