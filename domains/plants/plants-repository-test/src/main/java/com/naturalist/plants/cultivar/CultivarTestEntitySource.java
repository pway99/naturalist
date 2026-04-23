package com.naturalist.plants.cultivar;

import com.naturalist.data.TestEntitySource;

public class CultivarTestEntitySource extends TestEntitySource<CultivarName, Cultivar> {

    public CultivarTestEntitySource() {
        loadFile("plants/cultivar/cultivars.json");
    }
}
