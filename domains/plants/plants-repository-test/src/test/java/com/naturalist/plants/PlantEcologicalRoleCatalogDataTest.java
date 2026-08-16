package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Referential integrity for {@link PlantEcologicalRole#plantName()} — the check a
 * {@code ForeignKeyConstraint} would run if {@code plantName} referenced a single rank.
 * It does not: it is a {@link PlantRankName}, so this test resolves each role's taxon
 * against whichever rank catalog matches its rank. See
 * {@link PlantEcologicalRoleTestEntitySource} for why the declarative FK is absent.
 */
class PlantEcologicalRoleCatalogDataTest {

    private final NaturalistDatabase db = NaturalistDatabase.create();
    private final PlantRankResolution ranks = new PlantRankResolution(db);

    @Test
    void everyRoleResolvesToACataloguedTaxonAtItsRank() {
        assertThat(db.getNamed(PlantEcologicalRoleTestEntitySource.class).entityStream().toList())
                .allSatisfy(role -> assertThat(ranks.resolves(role.plantName()))
                        .as("role '%s' references %s taxon '%s'",
                                role.id().value(), role.plantName().rank(),
                                role.plantName().value())
                        .isTrue());
    }
}
