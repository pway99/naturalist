package com.naturalist.plants;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.observation.OrganismObservation;

import java.util.Collection;
import java.util.List;

/**
 * Namespace for the plants top-level sub-context's {@link BehavioralCollection}
 * return types. One file per namespace, nested types for everything inside (ADR-020).
 */
public interface PlantEntityCollections {

    final class SpeciesCollection extends BehavioralCollection<PlantSpecies> {

        SpeciesCollection(Collection<PlantSpecies> plants) {
            super(plants);
        }

        public static SpeciesCollection of(Collection<PlantSpecies> plants) {
            return new SpeciesCollection(plants);
        }

        public static SpeciesCollection empty() {
            return new SpeciesCollection(List.of());
        }
    }

    final class OrderCollection extends BehavioralCollection<PlantOrder> {

        OrderCollection(Collection<PlantOrder> orders) {
            super(orders);
        }

        public static OrderCollection of(Collection<PlantOrder> orders) {
            return new OrderCollection(orders);
        }

        public static OrderCollection empty() {
            return new OrderCollection(List.of());
        }
    }

    final class FamilyCollection extends BehavioralCollection<PlantFamily> {

        FamilyCollection(Collection<PlantFamily> families) {
            super(families);
        }

        public static FamilyCollection of(Collection<PlantFamily> families) {
            return new FamilyCollection(families);
        }

        public static FamilyCollection empty() {
            return new FamilyCollection(List.of());
        }
    }

    final class EcologicalRoleCollection extends BehavioralCollection<PlantEcologicalRole> {

        EcologicalRoleCollection(Collection<PlantEcologicalRole> roles) {
            super(roles);
        }

        public static EcologicalRoleCollection of(Collection<PlantEcologicalRole> roles) {
            return new EcologicalRoleCollection(roles);
        }

        public static EcologicalRoleCollection empty() {
            return new EcologicalRoleCollection(List.of());
        }
    }

    final class GenusCollection extends BehavioralCollection<PlantGenus> {

        GenusCollection(Collection<PlantGenus> genera) {
            super(genera);
        }

        public static GenusCollection of(Collection<PlantGenus> genera) {
            return new GenusCollection(genera);
        }

        public static GenusCollection empty() {
            return new GenusCollection(List.of());
        }
    }

    final class ImageCollection extends BehavioralCollection<PlantImage> {

        ImageCollection(Collection<PlantImage> images) {
            super(images);
        }

        public static ImageCollection of(Collection<PlantImage> images) {
            return new ImageCollection(images);
        }

        public static ImageCollection empty() {
            return new ImageCollection(List.of());
        }
    }

    final class ObservationCollection extends BehavioralCollection<OrganismObservation<PlantObservationId, PlantRankName>> {

        ObservationCollection(Collection<OrganismObservation<PlantObservationId, PlantRankName>> observations) {
            super(observations);
        }

        public static ObservationCollection of(Collection<OrganismObservation<PlantObservationId, PlantRankName>> observations) {
            return new ObservationCollection(observations);
        }

        public static ObservationCollection empty() {
            return new ObservationCollection(List.of());
        }
    }
}
