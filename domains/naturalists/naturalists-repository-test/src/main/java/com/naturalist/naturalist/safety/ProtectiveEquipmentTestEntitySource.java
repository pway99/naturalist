package com.naturalist.naturalist.safety;

import com.naturalist.data.TestEntitySource;
public class ProtectiveEquipmentTestEntitySource extends TestEntitySource<ProtectiveEquipmentId, ProtectiveEquipmentName, ProtectiveEquipment> {

    public ProtectiveEquipmentTestEntitySource() {
        super(ProtectiveEquipmentId::of);
        loadFile("naturalists/safety/protectiveEquipment.json");
    }
}
