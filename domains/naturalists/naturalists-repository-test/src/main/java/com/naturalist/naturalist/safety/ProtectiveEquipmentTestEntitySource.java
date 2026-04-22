package com.naturalist.naturalist.safety;

import com.naturalist.data.NamedTestEntitySource;

public class ProtectiveEquipmentTestEntitySource extends NamedTestEntitySource<ProtectiveEquipmentName, ProtectiveEquipment> {

    public ProtectiveEquipmentTestEntitySource() {
        loadFile("naturalists/safety/protectiveEquipment.json");
    }
}
