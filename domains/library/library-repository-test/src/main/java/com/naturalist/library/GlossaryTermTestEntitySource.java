package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class GlossaryTermTestEntitySource extends TestEntitySource<GlossaryTermName, GlossaryTerm> {

    public GlossaryTermTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("library/glossary-terms.json");
    }
}
