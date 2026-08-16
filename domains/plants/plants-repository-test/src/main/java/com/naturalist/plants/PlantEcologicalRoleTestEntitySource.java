package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

/**
 * Ecological roles for every catalogued taxon that has been characterised — 17 at species
 * rank, 5 at genus rank. The genus-rank records carry the roles of the five rows the
 * 2026-08-16 rank audit demoted, so the move cost no ecological data.
 * <p>
 * No {@code ForeignKeyConstraint}: {@code plantName} is a {@link PlantRankName} and the
 * framework's FK check resolves a single source class, while these point at two.
 * {@code PlantEcologicalRoleCatalogDataTest} covers the integrity instead.
 */
public class PlantEcologicalRoleTestEntitySource
        extends TestEntitySource<PlantEcologicalRoleId, PlantEcologicalRole> {

    public PlantEcologicalRoleTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plant-ecological-roles.json");
    }
}
