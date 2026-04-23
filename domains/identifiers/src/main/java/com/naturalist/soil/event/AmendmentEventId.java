package com.naturalist.soil.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class AmendmentEventId extends EntityId {

    private AmendmentEventId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static AmendmentEventId of(UUID value) {
        return new AmendmentEventId(value);
    }

    public static AmendmentEventId create() {
        return new AmendmentEventId(EntityId.newUUID());
    }
}
