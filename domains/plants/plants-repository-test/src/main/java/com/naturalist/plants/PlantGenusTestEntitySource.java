package com.naturalist.plants;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class PlantGenusTestEntitySource extends TestEntitySource<PlantGenusName, PlantGenus> {

    public PlantGenusTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plant-genera.json");
    }

    @Override
    protected List<ForeignKeyConstraint<PlantGenus, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "familyName",
                PlantGenus::familyName,
                PlantFamilyTestEntitySource.class));
    }
}
