package com.naturalist.plants;

import com.naturalist.ddd.ReadModel;
import com.naturalist.plants.PlantEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;

/**
 * The catalog-view read model for a plant at whatever rank identification reached —
 * its rank record composed with the photographic field record. Sealed across the four
 * botanical ranks that carry catalog entities, each a nested {@code record} permit (the
 * {@code permits} clause is inferred from the enclosing compilation unit):
 * {@link SpeciesView}, {@link GenusView}, {@link FamilyView}, {@link OrderView}.
 * Identity is the root rank's typed {@link PlantRankName}, returned polymorphically by
 * {@link #name()}. Permits carry no {@code features()} slot; features live on
 * {@link Plant} as an {@code OrganismFeatureView<PlantRankName, PlantFeature>} (later
 * chunk). Mirrors {@code InsectTaxonView}.
 */
public sealed interface PlantTaxonView extends ReadModel {

    /** The typed slug of the root rank record — polymorphic across the sealed permits. */
    PlantRankName name();

    /** Photographs of this organism at the root rank — non-null, possibly empty. */
    ImageCollection images();

    /** Species-rank permit. Root identity is the species's {@link PlantSpeciesName}. */
    record SpeciesView(PlantSpecies species, ImageCollection images) implements PlantTaxonView {

        public static SpeciesView of(PlantSpecies species, ImageCollection images) {
            return new SpeciesView(species, images);
        }

        public static SpeciesView of(PlantSpecies species) {
            return new SpeciesView(species, ImageCollection.empty());
        }

        @Override public PlantSpeciesName name() { return species.name(); }

        /** The genus this species belongs to, exposed as a typed FK delegate. */
        public PlantGenusName genusName() { return species.genusName(); }

        /** True iff this species's genus FK equals the given genus's name, OR the genus is null. */
        public boolean belongsToGenus(@Nullable GenusView genus) {
            return genus == null || species.genusName().equals(genus.name());
        }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.namedEntity(species, "species").behavioralCollection(images, "images");
        }
    }

    /** Genus-rank permit. Root identity is the genus's {@link PlantGenusName}. */
    record GenusView(PlantGenus genus, ImageCollection images) implements PlantTaxonView {

        public static GenusView of(PlantGenus genus, ImageCollection images) {
            return new GenusView(genus, images);
        }

        public static GenusView of(PlantGenus genus) {
            return new GenusView(genus, ImageCollection.empty());
        }

        @Override public PlantGenusName name() { return genus.name(); }

        /** The family this genus belongs to, exposed as a typed FK delegate. */
        public PlantFamilyName familyName() { return genus.familyName(); }

        /** True iff this genus's family FK equals the given family's name, OR the family is null. */
        public boolean belongsToFamily(@Nullable FamilyView family) {
            return family == null || genus.familyName().equals(family.name());
        }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.namedEntity(genus, "genus").behavioralCollection(images, "images");
        }
    }

    /** Family-rank permit. Root identity is the family's {@link PlantFamilyName}. */
    record FamilyView(PlantFamily family, ImageCollection images) implements PlantTaxonView {

        public static FamilyView of(PlantFamily family, ImageCollection images) {
            return new FamilyView(family, images);
        }

        public static FamilyView of(PlantFamily family) {
            return new FamilyView(family, ImageCollection.empty());
        }

        @Override public PlantFamilyName name() { return family.name(); }

        /** The order this family belongs to, exposed as a typed FK delegate. */
        public PlantOrderName orderName() { return family.orderName(); }

        /** True iff this family's order FK equals the given order's name, OR the order is null. */
        public boolean belongsToOrder(@Nullable OrderView order) {
            return order == null || family.orderName().equals(order.name());
        }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.namedEntity(family, "family").behavioralCollection(images, "images");
        }
    }

    /** Order-rank permit. Root identity is the order's {@link PlantOrderName}. */
    record OrderView(PlantOrder order, ImageCollection images) implements PlantTaxonView {

        public static OrderView of(PlantOrder order, ImageCollection images) {
            return new OrderView(order, images);
        }

        public static OrderView of(PlantOrder order) {
            return new OrderView(order, ImageCollection.empty());
        }

        @Override public PlantOrderName name() { return order.name(); }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.namedEntity(order, "order").behavioralCollection(images, "images");
        }
    }
}
