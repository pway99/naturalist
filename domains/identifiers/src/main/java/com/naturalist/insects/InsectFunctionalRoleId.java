package com.naturalist.insects;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class InsectFunctionalRoleId extends EntityId {
    private InsectFunctionalRoleId(UUID value) {
        super(value);
    }

    public static InsectFunctionalRoleId of(UUID value) {
        return new InsectFunctionalRoleId(value);
    }

    public static InsectFunctionalRoleId create() {
        return new InsectFunctionalRoleId(EntityId.newUUID());
    }
}
