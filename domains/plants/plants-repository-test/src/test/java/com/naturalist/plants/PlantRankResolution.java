package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test-side resolver for a {@link PlantRankName} against the four rank catalogs.
 * <p>
 * The framework's {@link com.naturalist.data.ForeignKeyConstraint} resolves a single
 * source class, so a component typed {@code PlantRankName} — one that attaches at
 * whichever rank the evidence supports — cannot declare one. This helper is what the
 * cross-rank consumers ({@code PlantEcologicalRole}, {@code PlantProgram},
 * {@code PhytochemicalConstituent}) use in their catalog-data tests to assert the same
 * referential integrity a declarative FK would have given a single-rank reference.
 */
public final class PlantRankResolution {

    private final Set<String> orders;
    private final Set<String> families;
    private final Set<String> genera;
    private final Set<String> species;

    public PlantRankResolution(NaturalistDatabase db) {
        this.orders = slugs(db, PlantOrderTestEntitySource.class);
        this.families = slugs(db, PlantFamilyTestEntitySource.class);
        this.genera = slugs(db, PlantGenusTestEntitySource.class);
        this.species = slugs(db, PlantSpeciesTestEntitySource.class);
    }

    private static <N extends com.naturalist.ddd.EntityName, E extends com.naturalist.ddd.NamedEntity<N>>
    Set<String> slugs(NaturalistDatabase db,
                      Class<? extends com.naturalist.data.TestEntitySource<N, E>> source) {
        return db.getNamed(source).entityStream()
                .map(e -> e.name().value())
                .collect(Collectors.toSet());
    }

    /** Whether {@code plantName} resolves to a catalogued record at its own rank. */
    public boolean resolves(PlantRankName plantName) {
        return switch (plantName.rank()) {
            case ORDER -> orders.contains(plantName.value());
            case FAMILY -> families.contains(plantName.value());
            case GENUS -> genera.contains(plantName.value());
            case SPECIES -> species.contains(plantName.value());
            default -> false;
        };
    }
}
