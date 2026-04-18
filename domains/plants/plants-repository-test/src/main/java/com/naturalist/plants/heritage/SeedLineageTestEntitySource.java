package com.naturalist.plants.heritage;

import com.naturalist.data.TestEntitySource;

public class SeedLineageTestEntitySource extends TestEntitySource<SeedLineageId, SeedLineageName, SeedLineage> {

    public SeedLineageTestEntitySource() {
        super(SeedLineageId::of);
        loadFile("plants/heritage/seed-lineages.json");
    }
}
