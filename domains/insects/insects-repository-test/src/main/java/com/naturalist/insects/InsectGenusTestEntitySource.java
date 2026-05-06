package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class InsectGenusTestEntitySource extends TestEntitySource<InsectGenusName, InsectGenus> {

    public InsectGenusTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-genera.json");
    }
}
