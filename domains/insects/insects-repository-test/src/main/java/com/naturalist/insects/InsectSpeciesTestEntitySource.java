package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class InsectSpeciesTestEntitySource extends TestEntitySource<InsectSpeciesName, InsectSpecies> {

    public InsectSpeciesTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-species.json");
    }
}
