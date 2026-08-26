package com.naturalist.usage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class UsageAlertId extends EntityId {
    private UsageAlertId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static UsageAlertId of(UUID value) {
        return new UsageAlertId(value);
    }

    public static UsageAlertId create() {
        return new UsageAlertId(EntityId.newUUID());
    }
}
