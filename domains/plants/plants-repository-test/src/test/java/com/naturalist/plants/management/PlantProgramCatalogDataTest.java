package com.naturalist.plants.management;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.plants.PlantRankResolution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Referential integrity for {@link PlantProgram#plantName()}. A program attaches at
 * whichever rank the evidence supports, so {@code plantName} is a
 * {@link com.naturalist.plants.PlantRankName} and cannot carry a single-source
 * {@code ForeignKeyConstraint}; this test resolves it against the matching rank catalog
 * instead. See {@link PlantProgramTestEntitySource}.
 */
class PlantProgramCatalogDataTest {

    @RegisterExtension
    private final NaturalistTestExtension db = NaturalistTestExtension.create();
    private final PlantRankResolution ranks = new PlantRankResolution(db);

    @Test
    void everyProgramResolvesToACataloguedTaxonAtItsRank() {
        assertThat(db.getNamed(PlantProgramTestEntitySource.class).entityStream().toList())
                .allSatisfy(program -> assertThat(ranks.resolves(program.plantName()))
                        .as("program '%s' references %s taxon '%s'",
                                program.name().value(), program.plantName().rank(),
                                program.plantName().value())
                        .isTrue());
    }
}
