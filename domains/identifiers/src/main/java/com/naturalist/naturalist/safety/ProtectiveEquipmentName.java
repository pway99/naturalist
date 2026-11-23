package com.naturalist.naturalist.safety;

import com.naturalist.ddd.EntityName;

public final class ProtectiveEquipmentName extends EntityName {

    private ProtectiveEquipmentName(String value) {
        super(value);
    }

    public static ProtectiveEquipmentName of(String value) {
        return new ProtectiveEquipmentName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
