package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PlantFeatureQueryImplTest {

    private PlantFeatureQueryImpl featureQuery() {
        NaturalistDatabase db = NaturalistDatabase.create();
        PlantQuery.GenusQuery genusQuery = new PlantGenusQueryImpl(new PlantGenusRepositoryMock(db));
        PlantQuery.SpeciesQuery speciesQuery =
                new PlantSpeciesQueryImpl(new PlantSpeciesRepositoryMock(db), genusQuery);
        PlantQuery.FamilyQuery familyQuery = new PlantFamilyQueryImpl(new PlantFamilyRepositoryMock(db));
        PlantAncestryResolver resolver = new PlantAncestryResolver(speciesQuery, genusQuery, familyQuery);
        return new PlantFeatureQueryImpl(
                new PlantFeatureRepositoryMock(db), new PlantFeatureAssignmentRepositoryMock(db), resolver);
    }

    @Test
    void findByRankName_composesAncestryFirst_ordinalOrderedWithinGroup() {
        // Seeded: ORDER asterales=[composite inflorescence]; FAMILY asteraceae=[ray florets(0),
        // composite inflorescence(1)]; GENUS helianthus=[ray florets]. Ancestor-first.
        PlantFeatureView view = featureQuery().findByRankName(PlantGenusName.of("helianthus"));

        assertThat(view.subject()).isEqualTo(PlantGenusName.of("helianthus"));
        assertThat(view.groups().stream().map(g -> g.rank()))
                .containsExactly(
                        PlantOrderName.of("asterales"),
                        PlantFamilyName.of("asteraceae"),
                        PlantGenusName.of("helianthus"));
        assertThat(view.groups().get(1).features().stream().map(PlantFeature::value))
                .containsExactly("ray florets", "composite inflorescence"); // ordinal 0 then 1
        assertThat(view.groups().get(2).features().stream().map(PlantFeature::value))
                .containsExactly("ray florets");
    }

    @Test
    void findByRankName_noAssignmentsInLineage_returnsEmptyGroups() {
        // piperales carries no feature assignments anywhere in its lineage.
        PlantFeatureView view = featureQuery().findByRankName(PlantOrderName.of("piperales"));
        assertThat(view.groups()).isEmpty();
    }

    @Test
    void findByRankName_rejectsNull() {
        assertThat(catchThrowable(() -> featureQuery().findByRankName(null)))
                .isInstanceOf(com.naturalist.exception.InvariantViolationException.class);
    }
}
