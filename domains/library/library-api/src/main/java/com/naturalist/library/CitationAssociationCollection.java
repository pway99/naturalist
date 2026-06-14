package com.naturalist.library;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public final class CitationAssociationCollection extends BehavioralCollection<CitationAssociation> {

    CitationAssociationCollection(Collection<CitationAssociation> associations) {
        super(associations);
    }

    public static CitationAssociationCollection of(Collection<CitationAssociation> associations) {
        return new CitationAssociationCollection(associations);
    }

    public static CitationAssociationCollection empty() {
        return new CitationAssociationCollection(List.of());
    }
}
