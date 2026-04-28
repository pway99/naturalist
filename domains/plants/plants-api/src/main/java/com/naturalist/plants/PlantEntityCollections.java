package com.naturalist.plants;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Namespace for the plants top-level sub-context's {@link BehavioralCollection}
 * return types. One file per namespace, nested types for everything inside (ADR-020).
 */
public interface PlantEntityCollections {

    final class PlantCollection extends BehavioralCollection<Plant> {

        PlantCollection(Collection<Plant> plants) {
            super(plants);
        }

        public static PlantCollection of(Collection<Plant> plants) {
            return new PlantCollection(plants);
        }

        public static PlantCollection empty() {
            return new PlantCollection(List.of());
        }
    }
}
