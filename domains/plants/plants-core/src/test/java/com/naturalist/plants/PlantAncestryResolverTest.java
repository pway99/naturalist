package com.naturalist.plants;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class PlantAncestryResolverTest {

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    private PlantAncestryResolver resolver() {
        PlantQuery.GenusQuery genusQuery = new PlantGenusQueryImpl(new PlantGenusRepositoryMock(nte));
        PlantQuery.SpeciesQuery speciesQuery =
                new PlantSpeciesQueryImpl(new PlantSpeciesRepositoryMock(nte), genusQuery);
        PlantQuery.FamilyQuery familyQuery = new PlantFamilyQueryImpl(new PlantFamilyRepositoryMock(nte));
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
