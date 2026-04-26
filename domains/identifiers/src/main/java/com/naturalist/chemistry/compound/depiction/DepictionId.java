package com.naturalist.chemistry.compound.depiction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class DepictionId extends EntityId {
    private DepictionId(UUID value) { super(value); }

    @JsonCreator
    public static DepictionId of(UUID value) { return new DepictionId(value); }

    public static DepictionId create() {
        return new DepictionId(EntityId.newUUID());
    }
}
