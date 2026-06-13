package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public final class CitationCollection extends BehavioralCollection<Citation> {

    CitationCollection(Collection<Citation> citations) {
        super(citations);
    }

    public static CitationCollection of(Collection<Citation> citations) {
        return new CitationCollection(citations);
    }

    public static CitationCollection empty() {
        return new CitationCollection(List.of());
    }
}
