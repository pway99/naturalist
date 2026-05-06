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

    final class PlantFamilyCollection extends BehavioralCollection<PlantFamily> {

        PlantFamilyCollection(Collection<PlantFamily> families) {
            super(families);
        }

        public static PlantFamilyCollection of(Collection<PlantFamily> families) {
            return new PlantFamilyCollection(families);
        }

        public static PlantFamilyCollection empty() {
            return new PlantFamilyCollection(List.of());
        }
    }

    final class PlantGenusCollection extends BehavioralCollection<PlantGenus> {

        PlantGenusCollection(Collection<PlantGenus> genera) {
            super(genera);
        }

        public static PlantGenusCollection of(Collection<PlantGenus> genera) {
            return new PlantGenusCollection(genera);
        }

        public static PlantGenusCollection empty() {
            return new PlantGenusCollection(List.of());
        }
    }
}
