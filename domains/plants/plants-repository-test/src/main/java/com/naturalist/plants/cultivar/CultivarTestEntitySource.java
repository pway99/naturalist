package com.naturalist.plants.cultivar;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class CultivarTestEntitySource extends TestEntitySource<CultivarName, Cultivar> {

    public CultivarTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/cultivar/cultivars.json");
    }
}
