package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class InsectImageId extends EntityId {
    private InsectImageId(UUID value) { super(value); }

    @JsonCreator
    public static InsectImageId of(UUID value) { return new InsectImageId(value); }

    public static InsectImageId create() {
        return new InsectImageId(EntityId.newUUID());
    }
}
