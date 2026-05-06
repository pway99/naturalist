package com.naturalist.naturalist;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class NaturalistTestEntitySource extends TestEntitySource<NaturalistName, Naturalist> {

    public NaturalistTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("naturalists/naturalists.json");
    }
}
