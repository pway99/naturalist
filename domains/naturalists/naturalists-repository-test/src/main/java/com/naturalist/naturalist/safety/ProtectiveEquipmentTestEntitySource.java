package com.naturalist.naturalist.safety;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class ProtectiveEquipmentTestEntitySource extends TestEntitySource<ProtectiveEquipmentName, ProtectiveEquipment> {

    public ProtectiveEquipmentTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("naturalists/safety/protectiveEquipment.json");
    }
}
