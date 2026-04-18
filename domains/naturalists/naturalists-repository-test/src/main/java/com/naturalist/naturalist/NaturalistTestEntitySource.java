package com.naturalist.naturalist;

import com.naturalist.data.TestEntitySource;
public class NaturalistTestEntitySource extends TestEntitySource<NaturalistId, NaturalistName, Naturalist> {

    public NaturalistTestEntitySource() {
        super(NaturalistId::of);
        loadFile("naturalists/naturalists.json");
    }
}
