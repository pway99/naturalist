package com.naturalist.insects;

import com.naturalist.observability.Observer;
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
 * the leaf classes (e.g. {@code InsectSpeciesInfo.name} typed {@code InsectSpeciesName})
 * serialize as plain strings via the {@link com.naturalist.ddd.EntityName}
 * {@code @JsonValue} on {@code value()}.
 */
public sealed interface InsectRankName extends RankName
        permits InsectOrderName, InsectFamilyName, InsectGenusName, InsectSpeciesName, InsectSubspeciesName {

    /**
     * Creates the appropriate {@code InsectRankName} permit for the given slug
     * and Linnaean rank. Only the five insect-side ranks are supported. A slug that
     * is not kebab-case, a null rank, or a higher rank (Kingdom, Phylum, Class) is
     * observed and refused with an
     * {@link com.naturalist.exception.InvariantViolationException}.
     */
    static InsectRankName of(String slug, LinealRank rank) {
        Observer.forClass(InsectRankName.class).arguments("of", i -> i
                .kebabFormat(slug, "slug")
                .notNull(rank, "rank")
                .isTrue(isInsectRank(rank), "insectRank"))
                .throwWhenInvalid();
        return switch (rank) {
            case ORDER -> InsectOrderName.of(slug);
            case FAMILY -> InsectFamilyName.of(slug);
            case GENUS -> InsectGenusName.of(slug);
            case SPECIES -> InsectSpeciesName.of(slug);
            case SUBSPECIES -> InsectSubspeciesName.of(slug);
            case KINGDOM, PHYLUM, CLASS -> throw new IllegalStateException("unreachable: " + rank);
        };
    }

    /**
     * Whether {@code rank} is one of the five ranks the insects catalogue names; null is not.
     * Every {@link LinealRank} is listed, so a new rank stops this compiling until it is placed.
     */
    static boolean isInsectRank(LinealRank rank) {
        return rank != null && switch (rank) {
            case ORDER, FAMILY, GENUS, SPECIES, SUBSPECIES -> true;
            case KINGDOM, PHYLUM, CLASS -> false;
        };
    }
}
