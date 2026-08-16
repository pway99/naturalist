package com.naturalist.plants;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class PlantSpeciesTestEntitySource extends TestEntitySource<PlantSpeciesName, PlantSpecies> {

    public PlantSpeciesTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plant-species.json");
    }

    @Override
    protected List<ForeignKeyConstraint<PlantSpecies, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "genusName",
                PlantSpecies::genusName,
                PlantGenusTestEntitySource.class));
    }
}
