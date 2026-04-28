package com.naturalist.plants.cultivar;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Namespace for the cultivar sub-context's {@link BehavioralCollection}
 * return types. One file per namespace, nested types for everything inside (ADR-020).
 */
public interface CultivarEntityCollections {

    final class CultivarCollection extends BehavioralCollection<Cultivar> {

        CultivarCollection(Collection<Cultivar> cultivars) {
            super(cultivars);
        }

        public static CultivarCollection of(Collection<Cultivar> cultivars) {
            return new CultivarCollection(cultivars);
        }

        public static CultivarCollection empty() {
            return new CultivarCollection(List.of());
        }
    }
}
