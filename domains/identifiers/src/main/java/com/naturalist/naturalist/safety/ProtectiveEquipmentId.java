package com.naturalist.naturalist.safety;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class ProtectiveEquipmentId extends PersistenceId<Long> {

    private ProtectiveEquipmentId(Long value) {
        super(value);
    }

    @JsonCreator
    public static ProtectiveEquipmentId of(Long value) {
        return new ProtectiveEquipmentId(value);
    }
}
