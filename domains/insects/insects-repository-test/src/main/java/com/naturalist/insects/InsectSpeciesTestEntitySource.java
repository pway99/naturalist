package com.naturalist.insects;

import com.naturalist.data.TestEntitySource;

public class InsectSpeciesTestEntitySource extends TestEntitySource<InsectSpeciesName, InsectSpecies> {

    public InsectSpeciesTestEntitySource() {
        loadFile("insects/insect-species.json");
    }
}
