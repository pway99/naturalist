package com.naturalist.soil.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class TillageEventId extends EntityId {

    private TillageEventId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static TillageEventId of(UUID value) {
        return new TillageEventId(value);
    }

    public static TillageEventId create() {
        return new TillageEventId(EntityId.newUUID());
    }
}
