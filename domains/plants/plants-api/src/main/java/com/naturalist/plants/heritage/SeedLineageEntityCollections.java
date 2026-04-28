package com.naturalist.plants.heritage;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Namespace for the heritage sub-context's {@link BehavioralCollection}
 * return types. One file per namespace, nested types for everything inside (ADR-020).
 */
public interface SeedLineageEntityCollections {

    final class SeedLineageCollection extends BehavioralCollection<SeedLineage> {

        SeedLineageCollection(Collection<SeedLineage> lineages) {
            super(lineages);
        }

        public static SeedLineageCollection of(Collection<SeedLineage> lineages) {
            return new SeedLineageCollection(lineages);
        }

        public static SeedLineageCollection empty() {
            return new SeedLineageCollection(List.of());
        }
    }
}
