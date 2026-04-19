package com.naturalist.insects;

import com.naturalist.data.NamedTestEntitySource;

public class InsectSpeciesTestEntitySource extends NamedTestEntitySource<InsectSpeciesName, InsectSpecies> {

    public InsectSpeciesTestEntitySource() {
        loadFile("insects/insects.json");
    }
}
