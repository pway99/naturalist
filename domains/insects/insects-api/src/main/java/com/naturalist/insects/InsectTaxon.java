package com.naturalist.insects;

import com.naturalist.observation.OrganismImage;

import com.naturalist.ddd.ReadModel;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;

/**
 * An insect taxon at Oak Vista — its stored rank record (order, family, genus or species,
 * depending on identification confidence) composed with the photographs attached at that
 * rank, as one read-side model.
 *
 * <p>Sealed across the four Linnaean ranks that currently carry catalog entities, each
 * a top-level {@code record} composing the stored {@code *Info} rank record with its
 * photographs:
 * <ul>
 *   <li>{@link InsectSpecies} — species-rank root (e.g. <i>Battus philenor</i>).</li>
 *   <li>{@link InsectGenus}   — genus-rank root, used when identification
 *       firmed up to genus but not species (e.g. <i>Empoasca</i>).</li>
 *   <li>{@link InsectFamily}  — family-rank root, used when only family
 *       is resolved (e.g. <i>Tachinidae</i>).</li>
 *   <li>{@link InsectOrder}   — order-rank root, used when only order
 *       is resolved (e.g. <i>Diptera</i> sp.).</li>
 * </ul>
 *
 * <p>The taxon's identity is the root rank's typed slug, returned by {@link #name()}
 * as the sealed {@link InsectRankName}. Consumers dispatch by pattern-matching the
 * sealed permit:
 *
 * <pre>{@code
 * switch (taxon) {
 *     case InsectSpecies s -> ...s.info()...;
 *     case InsectGenus   g -> ...g.info()...;
 *     case InsectFamily  f -> ...f.info()...;
 *     case InsectOrder   o -> ...o.info()...;
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
 * permit here — no {@code InsectSubspeciesInfo} entity exists yet. The factory
 * returns {@code Optional.empty()} for subspecies-rank requests; this interface
 * gains an {@code InsectSubspecies} permit when the entity lands.
 */
public sealed interface InsectTaxon extends ReadModel
        permits InsectSpecies, InsectGenus, InsectFamily, InsectOrder {

    /** The typed slug of the root rank record — polymorphic across the sealed permits. */
    InsectRankName name();

    /** Photographs of this organism at the root rank — non-null, possibly empty. */
    ImageCollection images();
}
