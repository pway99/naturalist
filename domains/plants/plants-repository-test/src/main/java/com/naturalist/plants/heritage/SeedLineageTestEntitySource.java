package com.naturalist.plants.heritage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class SeedLineageTestEntitySource extends TestEntitySource<SeedLineageName, SeedLineage> {

    public SeedLineageTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/heritage/seed-lineages.json");
    }
}
