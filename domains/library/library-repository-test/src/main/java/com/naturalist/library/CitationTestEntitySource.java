package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.NaturalistDatabase;

public class CitationTestEntitySource extends TestEntitySource<CitationName, Citation> {

    public CitationTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("library/citations.json");
    }
}
