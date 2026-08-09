package com.naturalist.library;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public final class GlossaryTermCollection extends BehavioralCollection<GlossaryTerm> {

    GlossaryTermCollection(Collection<GlossaryTerm> terms) {
        super(terms);
    }

    public static GlossaryTermCollection of(Collection<GlossaryTerm> terms) {
        return new GlossaryTermCollection(terms);
    }

    public static GlossaryTermCollection empty() {
        return new GlossaryTermCollection(List.of());
    }
}
