package com.naturalist.usage;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class UsageEventId extends EntityId {

    private UsageEventId(UUID value) {
        super(value);
    }

    public static UsageEventId of(UUID value) {
        return new UsageEventId(value);
    }

    public static UsageEventId create() {
        return new UsageEventId(EntityId.newUUID());
    }
}
