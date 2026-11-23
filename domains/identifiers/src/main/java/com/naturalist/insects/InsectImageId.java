package com.naturalist.insects;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class InsectImageId extends EntityId {
    private InsectImageId(UUID value) {
        super(value);
    }

    public static InsectImageId of(UUID value) {
        return new InsectImageId(value);
    }

    public static InsectImageId create() {
        return new InsectImageId(EntityId.newUUID());
    }
}
