package com.naturalist.insects;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class InsectSpeciesTestEntitySource extends TestEntitySource<InsectSpeciesName, InsectSpecies> {

    public InsectSpeciesTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-species.json");
    }

    @Override
    protected List<ForeignKeyConstraint<InsectSpecies, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "genusName",
                InsectSpecies::genusName,
                InsectGenusTestEntitySource.class));
    }
}
