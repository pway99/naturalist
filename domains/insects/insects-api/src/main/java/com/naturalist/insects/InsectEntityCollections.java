package com.naturalist.insects;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Namespace for the insects bounded context's {@link BehavioralCollection} return types.
 *
 * <p>Nested collections scope to a single entity each:
 * <ul>
 *   <li>{@link SpeciesCollection} — multi-result return type for {@link InsectSpecies}.</li>
 *   <li>{@link ImageCollection} — multi-result return type for {@link InsectImage}.</li>
 *   <li>{@link FamilyCollection} — multi-result return type for {@link InsectFamily}.</li>
 *   <li>{@link GenusCollection} — multi-result return type for {@link InsectGenus}.</li>
 *   <li>{@link FunctionalRoleCollection} — multi-result return type for
 *       {@link InsectFunctionalRole}.</li>
 * </ul>
 *
 * <p>{@code InsectEntityCollections} is a pure container — it holds no behavior of its own,
 * mirroring the {@link InsectQuery} pattern: one file per namespace, nested types for
 * everything inside.
 */
public interface InsectEntityCollections {

    final class SpeciesCollection extends BehavioralCollection<InsectSpecies> {

        SpeciesCollection(Collection<InsectSpecies> species) {
            super(species);
        }

        public static SpeciesCollection of(Collection<InsectSpecies> species) {
            return new SpeciesCollection(species);
        }

        public static SpeciesCollection empty() {
            return new SpeciesCollection(List.of());
        }
    }

    final class ImageCollection extends BehavioralCollection<InsectImage> {

        ImageCollection(Collection<InsectImage> images) {
            super(images);
        }

        public static ImageCollection of(Collection<InsectImage> images) {
            return new ImageCollection(images);
        }

        public static ImageCollection empty() {
            return new ImageCollection(List.of());
        }
    }

    final class FamilyCollection extends BehavioralCollection<InsectFamily> {

        FamilyCollection(Collection<InsectFamily> families) {
            super(families);
        }

        public static FamilyCollection of(Collection<InsectFamily> families) {
            return new FamilyCollection(families);
        }

        public static FamilyCollection empty() {
            return new FamilyCollection(List.of());
        }
    }

    final class GenusCollection extends BehavioralCollection<InsectGenus> {

        GenusCollection(Collection<InsectGenus> genera) {
            super(genera);
        }

        public static GenusCollection of(Collection<InsectGenus> genera) {
            return new GenusCollection(genera);
        }

        public static GenusCollection empty() {
            return new GenusCollection(List.of());
        }
    }

    final class FunctionalRoleCollection extends BehavioralCollection<InsectFunctionalRole> {

        FunctionalRoleCollection(Collection<InsectFunctionalRole> roles) {
            super(roles);
        }

        public static FunctionalRoleCollection of(Collection<InsectFunctionalRole> roles) {
            return new FunctionalRoleCollection(roles);
        }

        public static FunctionalRoleCollection empty() {
            return new FunctionalRoleCollection(List.of());
        }
    }
}
