package com.naturalist.chemistry.element;

import com.naturalist.data.TestEntitySource;
public class ElementTestEntitySource extends TestEntitySource<ElementId, ElementName, Element> {
    public ElementTestEntitySource() {
        super(ElementId::of);
        loadFile("chemistry/element/elements.json");
    }
}
