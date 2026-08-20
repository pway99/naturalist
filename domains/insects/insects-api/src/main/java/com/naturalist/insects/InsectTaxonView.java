package com.naturalist.insects;

import com.naturalist.observation.OrganismImage;

import com.naturalist.ddd.ReadModel;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;

/**
 * The catalog-view read model for an insect at Oak Vista — its rank record (family,
 * genus, or species, depending on identification confidence) and the photographic
 * field record assembled into a single read-side view.
 *
 * <p>Sealed across the four Linnaean ranks that currently carry catalog entities:
 * <ul>
 *   <li>{@link InsectSpeciesView} — species-rank root (e.g. <i>Battus philenor</i>).</li>
 *   <li>{@link InsectGenusView}   — genus-rank root, used when identification
 *       firmed up to genus but not species (e.g. <i>Empoasca</i>).</li>
 *   <li>{@link InsectFamilyView}  — family-rank root, used when only family
 *       is resolved (e.g. <i>Tachinidae</i>).</li>
 *   <li>{@link InsectOrderView}   — order-rank root, used when only order
 *       is resolved (e.g. <i>Diptera</i> sp.).</li>
 * </ul>
 *
 * <p>The view's identity is the root rank's typed slug, returned by {@link #name()}
 * as the sealed {@link InsectRankName}. Consumers dispatch by pattern-matching the
 * sealed permit:
 *
 * <pre>{@code
 * switch (view) {
 *     case InsectSpeciesView sv -> ...sv.species()...;
 *     case InsectGenusView   gv -> ...gv.genus()...;
 *     case InsectFamilyView  fv -> ...fv.family()...;
 *     case InsectOrderView   ov -> ...ov.order()...;
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
 * gains a {@code InsectSubspeciesView} permit when the entity lands.
 */
public sealed interface InsectTaxonView extends ReadModel
        permits InsectOrderView, InsectFamilyView, InsectGenusView, InsectSpeciesView {

    /** The typed slug of the root rank record — polymorphic across the sealed permits. */
    InsectRankName name();

    /** Photographs of this organism at the root rank — non-null, possibly empty. */
    ImageCollection images();
}
