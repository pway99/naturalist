package com.naturalist.biogeography;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.naturalist.fieldnotes.Description;

/**
 * A named biogeographical area used to record where an organism is native,
 * where a field observation was made, or where a heritage selection program
 * is rooted.
 * <p>
 * Bioregions are a small, curated, controlled vocabulary — a sealed type
 * permitting only the regions the application explicitly recognises. Adding
 * a new bioregion is a deliberate code change rather than free-text data
 * entry, which keeps the catalog cartographically meaningful and prevents
 * variant spellings of the same place.
 * <p>
 * The naming and extent of each region tracks the EPA Level III ecoregion
 * boundaries where they coincide with vernacular usage; the vocabulary is
 * bioregional in intent (naturalist-friendly, watershed-and-community based)
 * but ecoregion-precise in geometry. If formal EPA codes ever become useful
 * for reporting, they can be added as a component on individual permits
 * without disturbing consumers.
 * <p>
 * Each permit carries a four-level Durrell {@link Description} so the
 * application can speak about a place at any audience level — preschool
 * through university.
 * <p>
 * The hierarchy is intentionally flat. A nested parent ({@code parent()}
 * returning a containing region) can be added when the application has a
 * concrete need to reason about containment, not before.
 *
 * <h2>Jackson</h2>
 * Serialised as the {@link #slug()} string ({@link JsonValue}); deserialised
 * via {@link #of(String)} ({@link JsonCreator}). Unknown slugs throw
 * {@link IllegalArgumentException}.
 */
public sealed interface Bioregion
        permits SacramentoValley,
                SouthernCascades,
                KlamathMountains,
                CoastRanges,
                SierraNevada,
                ModocPlateau {

    @JsonValue
    String slug();

    String displayName();

    Description description();

    @JsonCreator
    static Bioregion of(String slug) {
        return switch (slug) {
            case "sacramento-valley" -> new SacramentoValley();
            case "southern-cascades" -> new SouthernCascades();
            case "klamath-mountains" -> new KlamathMountains();
            case "coast-ranges"      -> new CoastRanges();
            case "sierra-nevada"     -> new SierraNevada();
            case "modoc-plateau"     -> new ModocPlateau();
            default -> throw new IllegalArgumentException("Unknown bioregion: " + slug);
        };
    }
}
