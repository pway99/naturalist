package com.naturalist.plants.cultivar;

import com.naturalist.data.TestEntitySource;

public class CultivarTestEntitySource extends TestEntitySource<CultivarId, CultivarName, Cultivar> {

    public CultivarTestEntitySource() {
        super(CultivarId::of);
        loadFile("plants/cultivar/cultivars.json");
    }
}
