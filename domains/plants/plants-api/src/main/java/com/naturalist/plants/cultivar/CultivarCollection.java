package com.naturalist.plants.cultivar;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/** Cultivar collection (N=1 collapse: top-level, no *EntityCollections namespace). */
public final class CultivarCollection extends BehavioralCollection<Cultivar> {

    CultivarCollection(Collection<Cultivar> items) {
        super(items);
    }

    public static CultivarCollection of(Collection<Cultivar> items) {
        return new CultivarCollection(items);
    }

    public static CultivarCollection empty() {
        return new CultivarCollection(List.of());
    }
}
