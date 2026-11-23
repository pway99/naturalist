package com.naturalist.usage;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class UsageCounterId extends EntityId {

    private UsageCounterId(UUID value) {
        super(value);
    }

    public static UsageCounterId of(UUID value) {
        return new UsageCounterId(value);
    }

    public static UsageCounterId create() {
        return new UsageCounterId(EntityId.newUUID());
    }
}
