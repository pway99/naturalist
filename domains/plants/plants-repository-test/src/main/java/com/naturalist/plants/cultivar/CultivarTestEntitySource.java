package com.naturalist.plants.cultivar;

import com.naturalist.data.NamedTestEntitySource;

public class CultivarTestEntitySource extends NamedTestEntitySource<CultivarName, Cultivar> {

    public CultivarTestEntitySource() {
        loadFile("plants/cultivar/cultivars.json");
    }
}
