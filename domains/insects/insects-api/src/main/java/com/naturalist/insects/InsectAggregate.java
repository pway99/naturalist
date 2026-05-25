package com.naturalist.insects;

import com.naturalist.ddd.Aggregate;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;

/**
 * The catalog-view aggregate for an insect at Oak Vista — its rank record (family,
 * genus, or species, depending on identification confidence) and the photographic
 * field record assembled into a single consistency boundary.
 *
 * <p>Sealed across the four Linnaean ranks that currently carry catalog entities:
 * <ul>
 *   <li>{@link InsectSpeciesAggregate} — species-rank root (e.g. <i>Battus philenor</i>).</li>
 *   <li>{@link InsectGenusAggregate}   — genus-rank root, used when identification
 *       firmed up to genus but not species (e.g. <i>Empoasca</i>).</li>
 *   <li>{@link InsectFamilyAggregate}  — family-rank root, used when only family
 *       is resolved (e.g. <i>Tachinidae</i>).</li>
 *   <li>{@link InsectOrderAggregate}   — order-rank root, used when only order
 *       is resolved (e.g. <i>Diptera</i> sp.).</li>
 * </ul>
 *
 * <p>The aggregate's identity is the root rank's typed slug, returned by {@link #name()}
 * as the sealed {@link InsectRankName}. Consumers dispatch by pattern-matching the
 * sealed permit:
 *
 * <pre>{@code
 * switch (aggregate) {
 *     case InsectSpeciesAggregate sa -> ...sa.species()...;
 *     case InsectGenusAggregate   ga -> ...ga.genus()...;
 *     case InsectFamilyAggregate  fa -> ...fa.family()...;
 *     case InsectOrderAggregate   oa -> ...oa.order()...;
 * }
 * }</pre>
 *
 * <p>{@link ImageCollection} is non-null but may be empty on every permit — a rank
 * record can be catalogued without photographs. Per-permit {@code invariants()}
 * enforce structural validity (presence and validity of the rank entity and the image
 * collection); referential integrity between {@link InsectImage#parentName()} and the
 * root rank's name is the assembly factory's responsibility, since the factory queries
 * images by that name and the match is tautological at construction time.
 *
 * <p>{@code InsectSubspeciesName} is a permit on {@link InsectRankName} but has no
 * aggregate permit here — no {@code InsectSubspecies} entity exists yet. The factory
 * returns {@code Optional.empty()} for subspecies-rank requests; this interface
 * gains a {@code InsectSubspeciesAggregate} permit when the entity lands.
 */
public sealed interface InsectAggregate extends Aggregate
        permits InsectOrderAggregate, InsectFamilyAggregate, InsectGenusAggregate, InsectSpeciesAggregate {

    /** The typed slug of the root rank record — polymorphic across the sealed permits. */
    InsectRankName name();

    /** Photographs of this organism at the root rank — non-null, possibly empty. */
    ImageCollection images();
}
