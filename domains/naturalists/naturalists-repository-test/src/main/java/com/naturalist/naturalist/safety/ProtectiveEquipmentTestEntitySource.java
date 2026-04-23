package com.naturalist.naturalist.safety;

import com.naturalist.data.TestEntitySource;

public class ProtectiveEquipmentTestEntitySource extends TestEntitySource<ProtectiveEquipmentName, ProtectiveEquipment> {

    public ProtectiveEquipmentTestEntitySource() {
        loadFile("naturalists/safety/protectiveEquipment.json");
    }
}
