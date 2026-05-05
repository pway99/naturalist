package com.naturalist.insects;

import com.naturalist.data.TestEntitySource;

public class InsectFamilyTestEntitySource extends TestEntitySource<InsectFamilyName, InsectFamily> {

    public InsectFamilyTestEntitySource() {
        loadFile("insects/insect-families.json");
    }
}
