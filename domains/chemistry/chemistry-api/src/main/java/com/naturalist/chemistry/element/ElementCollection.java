package com.naturalist.chemistry.element;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public final class ElementCollection extends BehavioralCollection<Element> {

    ElementCollection(Collection<Element> elements) {
        super(elements);
    }

    public static ElementCollection of(Collection<Element> elements) {
        return new ElementCollection(elements);
    }

    public static ElementCollection empty() {
        return new ElementCollection(List.of());
    }
}
