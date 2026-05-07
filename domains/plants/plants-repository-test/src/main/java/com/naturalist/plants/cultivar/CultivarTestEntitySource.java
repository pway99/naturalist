package com.naturalist.plants.cultivar;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.plants.PlantTestEntitySource;

import java.util.List;

public class CultivarTestEntitySource extends TestEntitySource<CultivarName, Cultivar> {

    public CultivarTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/cultivar/cultivars.json");
    }

    @Override
    protected List<ForeignKeyConstraint<Cultivar, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "plantName",
                Cultivar::plantName,
                PlantTestEntitySource.class));
    }
}
