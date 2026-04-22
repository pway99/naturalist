package com.naturalist.naturalist;

import com.naturalist.data.NamedTestEntitySource;

public class NaturalistTestEntitySource extends NamedTestEntitySource<NaturalistName, Naturalist> {

    public NaturalistTestEntitySource() {
        loadFile("naturalists/naturalists.json");
    }
}
