package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class InsectFamilyTestEntitySource extends TestEntitySource<InsectFamilyName, InsectFamily> {

    public InsectFamilyTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-families.json");
    }
}
