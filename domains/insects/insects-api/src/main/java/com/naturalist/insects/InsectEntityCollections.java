package com.naturalist.insects;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.ddd.BehavioralMap;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;

/**
 * Namespace for the insects bounded context's {@link BehavioralCollection} return types.
 *
 * <p>Nested collections scope to a single entity each:
 * <ul>
 *   <li>{@link SpeciesCollection} — multi-result return type for {@link InsectSpeciesInfo}.</li>
 *   <li>{@link ImageCollection} — multi-result return type for {@link OrganismImage}.</li>
 *   <li>{@link FamilyCollection} — multi-result return type for {@link InsectFamilyInfo}.</li>
 *   <li>{@link GenusCollection} — multi-result return type for {@link InsectGenusInfo}.</li>
 *   <li>{@link ObservationCollection} — multi-result return type for
 *       {@link OrganismObservation}.</li>
 *   <li>{@link FunctionalRoleCollection} — multi-result return type for
 *       {@link InsectFunctionalRole}.</li>
 *   <li>{@link OrderCollection} — multi-result return type for {@link InsectOrderInfo}.</li>
 * </ul>
 *
 * <p>{@code InsectEntityCollections} is a pure container — it holds no behavior of its own,
 * mirroring the {@link InsectQuery} pattern: one file per namespace, nested types for
 * everything inside.
 */
public interface InsectEntityCollections {

    final class SpeciesCollection extends BehavioralCollection<InsectSpeciesInfo> {

        SpeciesCollection(Collection<InsectSpeciesInfo> species) {
            super(species);
        }

        public static SpeciesCollection of(Collection<InsectSpeciesInfo> species) {
            return new SpeciesCollection(species);
        }

        public static SpeciesCollection empty() {
            return new SpeciesCollection(List.of());
        }
    }

    final class ImageCollection extends BehavioralCollection<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> {

        ImageCollection(Collection<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> images) {
            super(images);
        }

        public static ImageCollection of(Collection<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> images) {
            return new ImageCollection(images);
        }

        public static ImageCollection empty() {
            return new ImageCollection(List.of());
        }
    }

    final class ObservationCollection extends BehavioralCollection<OrganismObservation<InsectObservationId, InsectRankName>> {

        ObservationCollection(Collection<OrganismObservation<InsectObservationId, InsectRankName>> observations) {
            super(observations);
        }

        public static ObservationCollection of(Collection<OrganismObservation<InsectObservationId, InsectRankName>> observations) {
            return new ObservationCollection(observations);
        }

        public static ObservationCollection empty() {
            return new ObservationCollection(List.of());
        }
    }

    final class ImageGallery extends BehavioralMap<InsectRankName, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> {

        ImageGallery(Collection<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> images) {
            super(images, OrganismImage::parentName);
        }

        ImageGallery(Map<InsectRankName, ? extends Collection<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>>> groups) {
            super(groups);
        }

        /**
         * Construct a gallery that auto-groups images by their {@link OrganismImage#parentName()}.
         * Use when the card entity matches the image parent rank (e.g. species cards).
         */
        public static ImageGallery of(Collection<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> images) {
            return new ImageGallery(images);
        }

        /**
         * Construct a gallery from a pre-computed grouping where the key is the card entity
         * name. Use when the card entity is a higher rank than the image parent (e.g. order
         * cards showing descendant species images).
         */
        public static ImageGallery grouped(Map<InsectRankName, ? extends Collection<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>>> groups) {
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

    final class FamilyCollection extends BehavioralCollection<InsectFamilyInfo> {

        FamilyCollection(Collection<InsectFamilyInfo> families) {
            super(families);
        }

        public static FamilyCollection of(Collection<InsectFamilyInfo> families) {
            return new FamilyCollection(families);
        }

        public static FamilyCollection empty() {
            return new FamilyCollection(List.of());
        }
    }

    final class GenusCollection extends BehavioralCollection<InsectGenusInfo> {

        GenusCollection(Collection<InsectGenusInfo> genera) {
            super(genera);
        }

        public static GenusCollection of(Collection<InsectGenusInfo> genera) {
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

        /** A new collection ordered by the parent rank's slug. */
        public FunctionalRoleCollection sortedByParentName() {
            return new FunctionalRoleCollection(stream()
                    .sorted(Comparator.comparing((InsectFunctionalRole role) -> role.parentName().value()))
                    .toList());
        }
    }

    final class OrderCollection extends BehavioralCollection<InsectOrderInfo> {

        OrderCollection(Collection<InsectOrderInfo> orders) {
            super(orders);
        }

        public static OrderCollection of(Collection<InsectOrderInfo> orders) {
            return new OrderCollection(orders);
        }

        public static OrderCollection empty() {
            return new OrderCollection(List.of());
        }
    }
}
