package com.naturalist.plants;

import com.naturalist.taxonomy.LinealRank;

/**
 * Sealed marker type for the three plant-side Linnaean rank names —
 * {@link PlantFamilyName}, {@link PlantGenusName}, {@link PlantSpeciesName}.
 *
 * <p>Used as the subject reference on records that may attach to any rank: a planting
 * whose variety is known only to genus, a phytochemical constituent recorded for a whole
 * genus, an observation identified to family. A single polymorphic field replaces one
 * nullable column per rank, and a later refinement — "this Salvia is <i>Salvia
 * officinalis</i>" — becomes a single-field update rather than a migration between
 * columns.
 *
 * <p>Three permits, not five. Plants catalogues no order-rank entity, and
 * {@code PlantSubspeciesName} does not exist; only a rank with a record behind it earns
 * a permit.
 *
 * <h2>Cultivar is not a permit</h2>
 *
 * A cultivated variety is a horticultural selection <em>within</em> a species, not a
 * rank below it. {@code CultivarName} therefore stays off this type, and a consumer
 * needing both carries them as two components — the same shape as an insect rank entity
 * carrying {@code name} alongside {@code placedIn}:
 *
 * <pre>{@code
 * Planting(PlantRankName subject, @Nullable CultivarName cultivarName, ...)
 * }</pre>
 *
 * Admitting it here would force {@link #rank()} to return null for one permit, and —
 * because a sealed type's permits must share a package in the unnamed module —
 * would drag {@code CultivarName} out of {@code com.naturalist.plants.cultivar}. Both
 * costs are the type reporting that the concept does not belong. See section D of
 * {@code docs/plans/organism-domain-blueprint.md}.
 *
 * <h2>Equality across permits</h2>
 *
 * Class-qualified, inherited from {@link com.naturalist.ddd.EntityName#equals(Object)}:
 * a {@code PlantGenusName} holding {@code "citrus"} never equals a {@code PlantSpeciesName}
 * holding the same string. That matters here more than on the insect side, because
 * plants' catalog has genuinely carried the same slug at two ranks.
 *
 * <h2>JSON</h2>
 *
 * Jackson polymorphic dispatch is configured <em>at the consuming field</em>, never on
 * this interface, so leaf-class serialization stays a plain slug via the
 * {@code @JsonValue} on {@code value()}. A consumer declares it on its component:
 *
 * <pre>{@code
 * @JsonTypeInfo(use = Id.NAME, property = "subjectRank", include = As.EXTERNAL_PROPERTY)
 * @JsonSubTypes({
 *     @Type(value = PlantFamilyName.class,  name = "FAMILY"),
 *     @Type(value = PlantGenusName.class,   name = "GENUS"),
 *     @Type(value = PlantSpeciesName.class, name = "SPECIES")
 * })
 * PlantRankName subject
 * }</pre>
 *
 * producing flat JSON with the discriminator as a sibling field:
 *
 * <pre>{@code
 * { "subjectRank": "GENUS", "subject": "salvia" }
 * }</pre>
 */
public sealed interface PlantRankName
        permits PlantFamilyName, PlantGenusName, PlantSpeciesName {

    /**
     * The slug string carried by this rank name. Exposed on the sealed interface so
     * polymorphic consumers can read the slug without down-casting to a permit; every
     * permit inherits the concrete implementation from
     * {@link com.naturalist.ddd.EntityName}.
     */
    String value();

    /**
     * The {@link LinealRank} position this permit occupies on the Linnaean ladder. Total
     * — every permit is a Linnaean rung by construction, so this never returns null. If
     * a candidate permit would need it to, that candidate is an orthogonal axis rather
     * than a rank.
     */
    LinealRank rank();

    /**
     * Creates the appropriate permit for the given slug and Linnaean rank. Only the
     * three ranks plants catalogues are supported; anything else throws.
     */
    static PlantRankName of(String slug, LinealRank rank) {
        return switch (rank) {
            case FAMILY -> PlantFamilyName.of(slug);
            case GENUS -> PlantGenusName.of(slug);
            case SPECIES -> PlantSpeciesName.of(slug);
            default -> throw new IllegalArgumentException("Unsupported plant rank: " + rank);
        };
    }
}
