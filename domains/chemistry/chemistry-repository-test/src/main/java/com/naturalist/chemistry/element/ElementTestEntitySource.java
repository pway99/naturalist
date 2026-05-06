package com.naturalist.chemistry.element;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class ElementTestEntitySource extends TestEntitySource<ElementName, Element> {
    public ElementTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("chemistry/element/elements.json");
    }
}
