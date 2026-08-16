package com.naturalist.plants.heritage;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/** SeedLineage collection (N=1 collapse: top-level, no *EntityCollections namespace). */
public final class SeedLineageCollection extends BehavioralCollection<SeedLineage> {

    SeedLineageCollection(Collection<SeedLineage> items) {
        super(items);
    }

    public static SeedLineageCollection of(Collection<SeedLineage> items) {
        return new SeedLineageCollection(items);
    }

    public static SeedLineageCollection empty() {
        return new SeedLineageCollection(List.of());
    }
}
