package com.naturalist.insects;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class InsectSpeciesTestEntitySource extends TestEntitySource<InsectSpeciesName, InsectSpeciesInfo> {

    public InsectSpeciesTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-species.json");
    }

    @Override
    protected List<ForeignKeyConstraint<InsectSpeciesInfo, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "genusName",
                InsectSpeciesInfo::genusName,
                InsectGenusTestEntitySource.class));
    }
}
