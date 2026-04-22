package com.naturalist.plants.heritage;

import com.naturalist.data.NamedTestEntitySource;

public class SeedLineageTestEntitySource extends NamedTestEntitySource<SeedLineageName, SeedLineage> {

    public SeedLineageTestEntitySource() {
        loadFile("plants/heritage/seed-lineages.json");
    }
}
