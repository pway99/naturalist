package com.naturalist.insects;

import com.naturalist.data.TestEntitySource;

public class InsectGenusTestEntitySource extends TestEntitySource<InsectGenusName, InsectGenus> {

    public InsectGenusTestEntitySource() {
        loadFile("insects/insect-genera.json");
    }
}
