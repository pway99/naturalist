package com.naturalist.insects;

import com.naturalist.observation.OrganismImage;

import com.naturalist.ddd.ReadModel;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The catalog-view read model for an insect at Oak Vista — its rank record (family,
 * genus, or species, depending on identification confidence) and the photographic
 * field record assembled into a single read-side view.
 *
 * <p>Sealed across the four Linnaean ranks that currently carry catalog entities, each
 * a nested {@code record} permit (the sealed {@code permits} clause is inferred from the
 * enclosing compilation unit):
 * <ul>
 *   <li>{@link SpeciesView} — species-rank root (e.g. <i>Battus philenor</i>).</li>
 *   <li>{@link GenusView}   — genus-rank root, used when identification
 *       firmed up to genus but not species (e.g. <i>Empoasca</i>).</li>
 *   <li>{@link FamilyView}  — family-rank root, used when only family
 *       is resolved (e.g. <i>Tachinidae</i>).</li>
 *   <li>{@link OrderView}   — order-rank root, used when only order
 *       is resolved (e.g. <i>Diptera</i> sp.).</li>
 * </ul>
 *
 * <p>The view's identity is the root rank's typed slug, returned by {@link #name()}
 * as the sealed {@link InsectRankName}. Consumers dispatch by pattern-matching the
 * sealed permit:
 *
 * <pre>{@code
 * switch (view) {
 *     case InsectTaxonView.SpeciesView sv -> ...sv.species()...;
 *     case InsectTaxonView.GenusView   gv -> ...gv.genus()...;
 *     case InsectTaxonView.FamilyView  fv -> ...fv.family()...;
 *     case InsectTaxonView.OrderView   ov -> ...ov.order()...;
 * }
 * }</pre>
 *
 * <p>{@link ImageCollection} is non-null but may be empty on every permit — a rank
 * record can be catalogued without photographs. Per-permit {@code invariants()}
 * enforce structural validity (presence and validity of the rank entity and the image
 * collection); referential integrity between {@link OrganismImage#parentName()} and the
 * root rank's name is the assembly factory's responsibility, since the factory queries
 * images by that name and the match is tautological at construction time.
 *
 * <p>{@code InsectSubspeciesName} is a permit on {@link InsectRankName} but has no
 * view permit here — no {@code InsectSubspecies} entity exists yet. The factory
 * returns {@code Optional.empty()} for subspecies-rank requests; this interface
 * gains a {@code SubspeciesView} permit when the entity lands.
 */
public sealed interface InsectTaxonView extends ReadModel {

    /** The typed slug of the root rank record — polymorphic across the sealed permits. */
    InsectRankName name();

    /** Photographs of this organism at the root rank — non-null, possibly empty. */
    ImageCollection images();

    /**
     * Species-rank permit — the canonical "we know exactly what species this is" view.
     * Composes the species record with the photographic field record. Root identity is
     * the species's {@link InsectSpeciesName}.
     */
    record SpeciesView(
            InsectSpecies species,
            ImageCollection images
    ) implements InsectTaxonView {

        public static SpeciesView of(InsectSpecies species, ImageCollection images) {
            return new SpeciesView(species, images);
        }

        public static SpeciesView of(InsectSpecies species) {
            return new SpeciesView(species, ImageCollection.empty());
        }

        @Override
        public InsectSpeciesName name() {
            return species.name();
        }

        /** The genus this species belongs to, exposed as a typed FK delegate. */
        public InsectGenusName genusName() {
            return species.genusName();
        }

        /**
         * True iff this species's genus FK equals the given genus's name, OR the
         * given genus is null. Null tolerance lets a caller compose this check
         * inside a {@code whenNotNull(species, ...)} block without firing a
         * redundant {@code isTrue} violation when the genus is missing.
         */
        public boolean belongsToGenus(@Nullable GenusView genus) {
            return genus == null || species.belongsToGenus(genus.name());
        }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .namedEntity(species, "species")
                    .behavioralCollection(images, "images");
        }
    }

    /**
     * Genus-rank permit — used when identification firmed up to genus but not to species
     * (e.g. <i>Empoasca</i> sp., <i>Halictus</i> sp.). Composes the genus record with the
     * photographic field record. Root identity is the genus's {@link InsectGenusName}.
     */
    record GenusView(
            InsectGenus genus,
            ImageCollection images
    ) implements InsectTaxonView {

        public static GenusView of(InsectGenus genus, ImageCollection images) {
            return new GenusView(genus, images);
        }

        public static GenusView of(InsectGenus genus) {
            return new GenusView(genus, ImageCollection.empty());
        }

        @Override
        public InsectGenusName name() {
            return genus.name();
        }

        /** The family this genus belongs to, exposed as a typed FK delegate. */
        public InsectFamilyName familyName() {
            return genus.familyName();
        }

        /**
         * True iff this genus's family FK equals the given family's name, OR the
         * given family is null. Null tolerance lets a caller compose this check
         * inside a {@code whenNotNull(genus, ...)} block without firing a
         * redundant {@code isTrue} violation when the family is missing
         * (monotonic-fill catches missing-ancestor via its own
         * {@code .genus:family} violation).
         */
        public boolean belongsToFamily(@Nullable FamilyView family) {
            return family == null || genus.belongsToFamily(family.name());
        }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .namedEntity(genus, "genus")
                    .behavioralCollection(images, "images");
        }
    }

    /**
     * Family-rank permit — used when only family is resolved (e.g. <i>Tachinidae</i> sp.,
     * <i>Braconidae</i> sp.). Composes the family record with the photographic field
     * record. Root identity is the family's {@link InsectFamilyName}.
     */
    record FamilyView(
            InsectFamily family,
            ImageCollection images
    ) implements InsectTaxonView {

        public static FamilyView of(InsectFamily family, ImageCollection images) {
            return new FamilyView(family, images);
        }

        public static FamilyView of(InsectFamily family) {
            return new FamilyView(family, ImageCollection.empty());
        }

        @Override
        public InsectFamilyName name() {
            return family.name();
        }

        /** The order this family belongs to, exposed as a typed FK delegate. */
        public InsectOrderName orderName() {
            return family.orderName();
        }

        /**
         * True iff this family's order FK equals the given order's name, OR the
         * given order is null. Null tolerance lets a caller compose this check
         * inside a {@code whenNotNull(family, ...)} block without firing a
         * redundant {@code isTrue} violation when the order is missing —
         * monotonic-fill already reports the missing-order case via its own
         * violation.
         */
        public boolean belongsToOrder(@Nullable OrderView order) {
            return order == null || family.belongsToOrder(order.name());
        }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .namedEntity(family, "family")
                    .behavioralCollection(images, "images");
        }
    }

    /**
     * Order-rank permit — used when only order is resolved (e.g. <i>Diptera</i> sp.).
     * Composes the order record with the photographic field record. Root identity is the
     * order's {@link InsectOrderName}.
     */
    record OrderView(
            InsectOrder order,
            ImageCollection images
    ) implements InsectTaxonView {

        public static OrderView of(InsectOrder order, ImageCollection images) {
            return new OrderView(order, images);
        }

        public static OrderView of(InsectOrder order) {
            return new OrderView(order, ImageCollection.empty());
        }

        @Override
        public InsectOrderName name() {
            return order.name();
        }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .namedEntity(order, "order")
                    .behavioralCollection(images, "images");
        }
    }
}
