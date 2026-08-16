package com.naturalist.plants;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Namespace for the plants top-level sub-context's {@link BehavioralCollection}
 * return types. One file per namespace, nested types for everything inside (ADR-020).
 */
public interface PlantEntityCollections {

    final class PlantSpeciesCollection extends BehavioralCollection<PlantSpecies> {

        PlantSpeciesCollection(Collection<PlantSpecies> plants) {
            super(plants);
        }

        public static PlantSpeciesCollection of(Collection<PlantSpecies> plants) {
            return new PlantSpeciesCollection(plants);
        }

        public static PlantSpeciesCollection empty() {
            return new PlantSpeciesCollection(List.of());
        }
    }

    final class PlantOrderCollection extends BehavioralCollection<PlantOrder> {

        PlantOrderCollection(Collection<PlantOrder> orders) {
            super(orders);
        }

        public static PlantOrderCollection of(Collection<PlantOrder> orders) {
            return new PlantOrderCollection(orders);
        }

        public static PlantOrderCollection empty() {
            return new PlantOrderCollection(List.of());
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

    final class PlantEcologicalRoleCollection extends BehavioralCollection<PlantEcologicalRole> {

        PlantEcologicalRoleCollection(Collection<PlantEcologicalRole> roles) {
            super(roles);
        }

        public static PlantEcologicalRoleCollection of(Collection<PlantEcologicalRole> roles) {
            return new PlantEcologicalRoleCollection(roles);
        }

        public static PlantEcologicalRoleCollection empty() {
            return new PlantEcologicalRoleCollection(List.of());
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
