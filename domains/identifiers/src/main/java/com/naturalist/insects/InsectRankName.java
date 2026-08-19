package com.naturalist.insects;

import com.naturalist.taxonomy.LinealRank;
import com.naturalist.taxonomy.RankName;

/**
 * Sealed marker type for the five insect-side Linnaean rank names —
 * {@link InsectOrderName}, {@link InsectFamilyName}, {@link InsectGenusName},
 * {@link InsectSpeciesName}, {@link InsectSubspeciesName}.
 *
 * <p>Used as the subject/parent reference type on records that may attach to any
 * rank. The canonical cases are the shared kernel evidence records
 * {@code OrganismObservation<InsectObservationId, InsectRankName>.subject} and
 * {@code OrganismImage<InsectImageId, InsectObservationId, InsectRankName>.parentName}:
 * a sighting or field photograph that identifies the organism to order, family,
 * genus, species, or subspecies — whichever rank the naturalist's confidence
 * allows. Rank transitions ("we now know this is <i>Empoasca fabae</i>, not just
 * an Empoasca") become a single-field update on the consumer record.
 *
 * <p>The sealed permit list is the type-system enforcement: only insect-side
 * rank names compile into a slot typed {@code InsectRankName}. Cross-domain
 * names (plants, chemistry, …) are statically excluded.
 *
 * <p>Equality across permits is class-qualified — the existing
 * {@link com.naturalist.ddd.EntityName#equals(Object)} contract returns
 * {@code false} when {@code getClass()} differs. A genus slug and a family
 * slug that happen to share the same string value (theoretical, since
 * Linnaean nomenclature distinguishes them) never compare equal at runtime.
 *
 * <h2>JSON</h2>
 *
 * {@code InsectRankName} extends the kernel {@link RankName}. The shared kernel
 * evidence records serialize their rank reference through the kernel's field-level
 * {@code @JsonSerialize(RankNameSerializer)} / {@code @JsonDeserialize(RankNameDeserializer)}
 * codec, producing a self-describing object:
 *
 * <pre>{@code
 * { "rank": "GENUS", "value": "empoasca" }
 * }</pre>
 *
 * On read the concrete permit is rebuilt by the domain's {@code RankNameReconstructor}
 * ({@code InsectRankName::of}) registered on the mapper. Domain-local consumers that
 * still declare field-level {@code @JsonSubTypes} dispatch remain valid; direct uses of
 * the leaf classes (e.g. {@code InsectSpecies.name} typed {@code InsectSpeciesName})
 * serialize as plain strings via the {@link com.naturalist.ddd.EntityName}
 * {@code @JsonValue} on {@code value()}.
 */
public sealed interface InsectRankName extends RankName
        permits InsectOrderName, InsectFamilyName, InsectGenusName, InsectSpeciesName, InsectSubspeciesName {

    /**
     * Creates the appropriate {@code InsectRankName} permit for the given slug
     * and Linnaean rank. Only the five insect-side ranks are supported; higher
     * ranks (Kingdom, Phylum, Class) throw {@link IllegalArgumentException}.
     */
    static InsectRankName of(String slug, LinealRank rank) {
        return switch (rank) {
            case ORDER -> InsectOrderName.of(slug);
            case FAMILY -> InsectFamilyName.of(slug);
            case GENUS -> InsectGenusName.of(slug);
            case SPECIES -> InsectSpeciesName.of(slug);
            case SUBSPECIES -> InsectSubspeciesName.of(slug);
            default -> throw new IllegalArgumentException("Unsupported insect rank: " + rank);
        };
    }
}
