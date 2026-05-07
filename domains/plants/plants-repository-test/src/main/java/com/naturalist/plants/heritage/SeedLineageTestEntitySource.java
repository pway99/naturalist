package com.naturalist.plants.heritage;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.plants.cultivar.CultivarTestEntitySource;

import java.util.List;

public class SeedLineageTestEntitySource extends TestEntitySource<SeedLineageName, SeedLineage> {

    public SeedLineageTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/heritage/seed-lineages.json");
    }

    @Override
    protected List<ForeignKeyConstraint<SeedLineage, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "cultivarName",
                SeedLineage::cultivarName,
                CultivarTestEntitySource.class));
    }
}
