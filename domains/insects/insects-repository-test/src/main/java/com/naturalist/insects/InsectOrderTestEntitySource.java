package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class InsectOrderTestEntitySource extends TestEntitySource<InsectOrderName, InsectOrder> {

    public InsectOrderTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-orders.json");
    }
}
