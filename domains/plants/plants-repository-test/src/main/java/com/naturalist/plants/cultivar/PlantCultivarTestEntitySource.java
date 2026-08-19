package com.naturalist.plants.cultivar;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.plants.PlantSpeciesTestEntitySource;

import java.util.List;

public class PlantCultivarTestEntitySource extends TestEntitySource<CultivarName, Cultivar> {

    public PlantCultivarTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/cultivar/cultivars.json");
    }

    @Override
    protected List<ForeignKeyConstraint<Cultivar, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "plantName",
                Cultivar::plantName,
                PlantSpeciesTestEntitySource.class));
    }
}
