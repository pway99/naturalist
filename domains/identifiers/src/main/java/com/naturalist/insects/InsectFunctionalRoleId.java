package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class InsectFunctionalRoleId extends EntityId {
    private InsectFunctionalRoleId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static InsectFunctionalRoleId of(UUID value) {
        return new InsectFunctionalRoleId(value);
    }

    public static InsectFunctionalRoleId create() {
        return new InsectFunctionalRoleId(EntityId.newUUID());
    }
}
