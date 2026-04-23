package com.naturalist.plants.heritage;

import com.naturalist.data.TestEntitySource;

public class SeedLineageTestEntitySource extends TestEntitySource<SeedLineageName, SeedLineage> {

    public SeedLineageTestEntitySource() {
        loadFile("plants/heritage/seed-lineages.json");
    }
}
