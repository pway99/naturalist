package com.naturalist.chemistry.element;

import com.naturalist.data.TestEntitySource;

public class ElementTestEntitySource extends TestEntitySource<ElementName, Element> {
    public ElementTestEntitySource() {
        loadFile("chemistry/element/elements.json");
    }
}
