package com.naturalist.plants.heritage;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.plants.cultivar.PlantCultivarTestEntitySource;

import java.util.List;

public class PlantSeedLineageTestEntitySource extends TestEntitySource<SeedLineageName, SeedLineage> {

    public PlantSeedLineageTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/heritage/seed-lineages.json");
    }

    @Override
    protected List<ForeignKeyConstraint<SeedLineage, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "cultivarName",
                SeedLineage::cultivarName,
                PlantCultivarTestEntitySource.class));
    }
}
