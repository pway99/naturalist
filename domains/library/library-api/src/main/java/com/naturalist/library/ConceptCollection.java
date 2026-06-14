package com.naturalist.library;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public final class ConceptCollection extends BehavioralCollection<Concept> {

    ConceptCollection(Collection<Concept> concepts) {
        super(concepts);
    }

    public static ConceptCollection of(Collection<Concept> concepts) {
        return new ConceptCollection(concepts);
    }

    public static ConceptCollection empty() {
        return new ConceptCollection(List.of());
    }
}
