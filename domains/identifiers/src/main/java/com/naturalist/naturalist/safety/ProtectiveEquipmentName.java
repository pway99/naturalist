package com.naturalist.naturalist.safety;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.CatalogName;

public final class ProtectiveEquipmentName extends CatalogName {

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
