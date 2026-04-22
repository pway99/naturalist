package com.naturalist.chemistry.element;

import com.naturalist.data.NamedTestEntitySource;

public class ElementTestEntitySource extends NamedTestEntitySource<ElementName, Element> {
    public ElementTestEntitySource() {
        loadFile("chemistry/element/elements.json");
    }
}
