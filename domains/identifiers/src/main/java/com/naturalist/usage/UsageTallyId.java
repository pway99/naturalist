package com.naturalist.usage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class UsageTallyId extends EntityId {
    private UsageTallyId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static UsageTallyId of(UUID value) {
        return new UsageTallyId(value);
    }

    public static UsageTallyId create() {
        return new UsageTallyId(EntityId.newUUID());
    }
}
