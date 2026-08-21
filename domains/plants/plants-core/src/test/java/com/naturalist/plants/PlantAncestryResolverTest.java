package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantAncestryResolverTest {

    private PlantAncestryResolver resolver() {
        NaturalistDatabase db = NaturalistDatabase.create();
        PlantQuery.GenusQuery genusQuery = new PlantGenusQueryImpl(new PlantGenusRepositoryMock(db));
        PlantQuery.SpeciesQuery speciesQuery =
                new PlantSpeciesQueryImpl(new PlantSpeciesRepositoryMock(db), genusQuery);
        PlantQuery.FamilyQuery familyQuery = new PlantFamilyQueryImpl(new PlantFamilyRepositoryMock(db));
        return new PlantAncestryResolver(speciesQuery, genusQuery, familyQuery);
    }

    @Test
    void ancestry_ofGenus_isAncestorFirst() {
        // helianthus → asteraceae → asterales; returned ancestor-first (order → … → subject).
        assertThat(resolver().ancestry(PlantGenusName.of("helianthus")))
                .containsExactly(
                        PlantOrderName.of("asterales"),
                        PlantFamilyName.of("asteraceae"),
                        PlantGenusName.of("helianthus"));
    }

    @Test
    void ancestry_ofOrder_isSelfOnly() {
        assertThat(resolver().ancestry(PlantOrderName.of("asterales")))
                .containsExactly(PlantOrderName.of("asterales"));
    }
}
