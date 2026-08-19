package com.naturalist.plants.phytochemistry;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantRankResolution;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Referential integrity for {@link PhytochemicalConstituent#plantName()}. A constituent
 * can be recorded for a genus (thymol against {@code thymus}) as readily as a species, so
 * {@code plantName} is a {@link com.naturalist.plants.PlantRankName} with no single-source
 * {@code ForeignKeyConstraint}; this test resolves it against the matching rank catalog.
 * The cross-domain {@code compoundName} reference is a service-layer rule and is not
 * checked here. See {@link PlantPhytochemicalConstituentTestEntitySource}.
 */
class PhytochemicalConstituentCatalogDataTest {

    private final NaturalistDatabase db = NaturalistDatabase.create();
    private final PlantRankResolution ranks = new PlantRankResolution(db);

    @Test
    void everyConstituentResolvesToACataloguedTaxonAtItsRank() {
        assertThat(db.getNamed(PlantPhytochemicalConstituentTestEntitySource.class)
                .entityStream().toList())
                .allSatisfy(constituent -> assertThat(ranks.resolves(constituent.plantName()))
                        .as("constituent '%s' references %s taxon '%s'",
                                constituent.name().value(), constituent.plantName().rank(),
                                constituent.plantName().value())
                        .isTrue());
    }
}
