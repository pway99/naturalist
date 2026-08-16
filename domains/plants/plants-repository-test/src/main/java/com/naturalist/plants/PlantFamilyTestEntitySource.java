package com.naturalist.plants;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class PlantFamilyTestEntitySource extends TestEntitySource<PlantFamilyName, PlantFamily> {

    public PlantFamilyTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plant-families.json");
    }

    @Override
    protected List<ForeignKeyConstraint<PlantFamily, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "orderName",
                PlantFamily::orderName,
                PlantOrderTestEntitySource.class));
    }
}
