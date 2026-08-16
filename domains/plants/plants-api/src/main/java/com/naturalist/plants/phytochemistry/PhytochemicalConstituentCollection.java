package com.naturalist.plants.phytochemistry;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/** PhytochemicalConstituent collection (N=1 collapse: top-level, no *EntityCollections namespace). */
public final class PhytochemicalConstituentCollection extends BehavioralCollection<PhytochemicalConstituent> {

    PhytochemicalConstituentCollection(Collection<PhytochemicalConstituent> items) {
        super(items);
    }

    public static PhytochemicalConstituentCollection of(Collection<PhytochemicalConstituent> items) {
        return new PhytochemicalConstituentCollection(items);
    }

    public static PhytochemicalConstituentCollection empty() {
        return new PhytochemicalConstituentCollection(List.of());
    }
}
