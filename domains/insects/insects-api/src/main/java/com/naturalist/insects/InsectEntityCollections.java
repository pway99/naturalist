package com.naturalist.insects;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.ddd.BehavioralMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.naturalist.observation.OrganismObservation;

/**
 * Namespace for the insects bounded context's {@link BehavioralCollection} return types.
 *
 * <p>Nested collections scope to a single entity each:
 * <ul>
 *   <li>{@link SpeciesCollection} — multi-result return type for {@link InsectSpecies}.</li>
 *   <li>{@link ImageCollection} — multi-result return type for {@link InsectImage}.</li>
 *   <li>{@link FeatureCollection} — multi-result return type for {@link InsectFeature}.</li>
 *   <li>{@link FamilyCollection} — multi-result return type for {@link InsectFamily}.</li>
 *   <li>{@link GenusCollection} — multi-result return type for {@link InsectGenus}.</li>
 *   <li>{@link InsectObservationCollection} — multi-result return type for
 *       {@link OrganismObservation}.</li>
 *   <li>{@link FunctionalRoleCollection} — multi-result return type for
 *       {@link InsectFunctionalRole}.</li>
 *   <li>{@link OrderCollection} — multi-result return type for {@link InsectOrder}.</li>
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

    final class InsectObservationCollection extends BehavioralCollection<OrganismObservation<InsectObservationId, InsectRankName>> {

        InsectObservationCollection(Collection<OrganismObservation<InsectObservationId, InsectRankName>> observations) {
            super(observations);
        }

        public static InsectObservationCollection of(Collection<OrganismObservation<InsectObservationId, InsectRankName>> observations) {
            return new InsectObservationCollection(observations);
        }

        public static InsectObservationCollection empty() {
            return new InsectObservationCollection(List.of());
        }
    }

    final class FeatureCollection extends BehavioralCollection<InsectFeature> {

        FeatureCollection(Collection<InsectFeature> features) {
            super(features);
        }

        public static FeatureCollection of(Collection<InsectFeature> features) {
            return new FeatureCollection(features);
        }

        public static FeatureCollection empty() {
            return new FeatureCollection(List.of());
        }
    }

    final class ImageGallery extends BehavioralMap<InsectRankName, InsectImage> {

        ImageGallery(Collection<InsectImage> images) {
            super(images, InsectImage::parentName);
        }

        ImageGallery(Map<InsectRankName, ? extends Collection<InsectImage>> groups) {
            super(groups);
        }

        /**
         * Construct a gallery that auto-groups images by their {@link InsectImage#parentName()}.
         * Use when the card entity matches the image parent rank (e.g. species cards).
         */
        public static ImageGallery of(Collection<InsectImage> images) {
            return new ImageGallery(images);
        }

        /**
         * Construct a gallery from a pre-computed grouping where the key is the card entity
         * name. Use when the card entity is a higher rank than the image parent (e.g. order
         * cards showing descendant species images).
         */
        public static ImageGallery grouped(Map<InsectRankName, ? extends Collection<InsectImage>> groups) {
            return new ImageGallery(groups);
        }

        public static ImageGallery empty() {
            return new ImageGallery(List.of());
        }

        /**
         * Returns the images grouped under the given entity name, or an empty
         * {@link ImageCollection} if no images exist for that entity.
         */
        public ImageCollection forEntity(InsectRankName name) {
            return ImageCollection.of(elementsForKey(name));
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

    final class OrderCollection extends BehavioralCollection<InsectOrder> {

        OrderCollection(Collection<InsectOrder> orders) {
            super(orders);
        }

        public static OrderCollection of(Collection<InsectOrder> orders) {
            return new OrderCollection(orders);
        }

        public static OrderCollection empty() {
            return new OrderCollection(List.of());
        }
    }
}
