package com.naturalist.naturalist.safety;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

public final class ProtectiveEquipmentName extends EntityName {

    private ProtectiveEquipmentName(String value) {
        super(value);
    }

    @JsonCreator
    public static ProtectiveEquipmentName of(String value) {
        return new ProtectiveEquipmentName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
