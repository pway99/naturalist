package com.naturalist.naturalist;

import com.naturalist.data.TestEntitySource;

public class NaturalistTestEntitySource extends TestEntitySource<NaturalistName, Naturalist> {

    public NaturalistTestEntitySource() {
        loadFile("naturalists/naturalists.json");
    }
}
