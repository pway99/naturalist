package com.naturalist.insects;

import com.naturalist.taxonomy.LinealRank;

/**
 * Sealed marker type for the four insect-side Linnaean rank names —
 * {@link InsectFamilyName}, {@link InsectGenusName}, {@link InsectSpeciesName},
 * {@link InsectSubspeciesName}.
 *
 * <p>Used as the parent reference type on records that may attach to any of
 * the four ranks. The canonical case is {@code InsectImage.parentName}:
 * a field photograph that identifies the organism to family, genus, species,
 * or subspecies — whichever rank the naturalist's confidence allows. Rank
 * transitions ("we now know this is <i>Empoasca fabae</i>, not just an
 * Empoasca") become a single-field update on the consumer record.
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
 * Jackson polymorphic dispatch is configured <em>at the consuming field</em>,
 * not on this interface, to avoid polluting every leaf-class serialization
 * site with an envelope wrapper. A consumer record declares the dispatch on
 * its component:
 *
 * <pre>{@code
 * public record InsectImage(
 *         ...
 *         @JsonTypeInfo(use = Id.NAME, property = "parentRank",
 *                       include = As.EXTERNAL_PROPERTY)
 *         @JsonSubTypes({
 *             @Type(value = InsectFamilyName.class,     name = "FAMILY"),
 *             @Type(value = InsectGenusName.class,      name = "GENUS"),
 *             @Type(value = InsectSpeciesName.class,    name = "SPECIES"),
 *             @Type(value = InsectSubspeciesName.class, name = "SUBSPECIES")
 *         })
 *         InsectRankName parentName,
 *         ...
 * ) { }
 * }</pre>
 *
 * Produces flat JSON with the discriminator as a sibling field:
 *
 * <pre>{@code
 * {
 *   "parentRank": "GENUS",
 *   "parentName": "empoasca"
 * }
 * }</pre>
 *
 * Direct uses of the leaf classes (e.g., {@code InsectSpecies.name} typed
 * {@code InsectSpeciesName}) continue to serialize as plain strings via the
 * {@link com.naturalist.ddd.EntityName} {@code @JsonValue} on {@code value()}.
 */
public sealed interface InsectRankName
        permits InsectFamilyName, InsectGenusName, InsectSpeciesName, InsectSubspeciesName {

    /**
     * The slug string carried by this rank name. Exposed on the sealed
     * interface so polymorphic consumers (e.g. the {@code /insects/guild/{guild}}
     * console page sorting by {@code parentName.value()}) can read the slug
     * without down-casting to a specific permit. Every permit inherits the
     * concrete implementation from {@link com.naturalist.ddd.EntityName}.
     */
    String value();

    /**
     * The {@link LinealRank} position this permit occupies on the Linnaean ladder.
     * Lets polymorphic consumers read the rank directly without {@code instanceof}
     * switching or reflective class-name inspection — the {@code guild.jte} rank
     * badge being the canonical case.
     */
    LinealRank rank();
}
