package com.naturalist.insects;

import com.naturalist.data.TestEntitySource;

public class InsectSpeciesTestEntitySource extends TestEntitySource<InsectSpeciesId, InsectSpeciesName, InsectSpecies> {

    public InsectSpeciesTestEntitySource() {
        super(InsectSpeciesId::of);
        loadFile("insects/insects.json");
    }
}
