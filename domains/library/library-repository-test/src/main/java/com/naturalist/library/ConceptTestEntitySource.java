package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class ConceptTestEntitySource extends TestEntitySource<ConceptName, Concept> {

    public ConceptTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("library/concepts.json");
    }
}
