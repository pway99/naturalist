package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PlantFeatureQueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    PlantFeatureRepositoryMock featureRepository = new PlantFeatureRepositoryMock(db);
    PlantFeatureAssignmentRepositoryMock assignmentRepository =
            new PlantFeatureAssignmentRepositoryMock(db);

    PlantFeatureQueryImpl featureQuery =
            new PlantFeatureQueryImpl(featureRepository, assignmentRepository);

    @Test
    void forRankName_returnsFeaturesAssignedDirectlyAtTheGivenRank() {
        // Seeded FAMILY-rank assignments for asteraceae: "ray florets" (ordinal 0) and
        // "composite inflorescence" (ordinal 1). See plant-feature-assignments.json.
        var result = featureQuery.forRankName(TestPlantsIdentifiers.PlantFamilies.Asteraceae.name);

        assertThat(result.stream().map(PlantFeature::value))
                .containsExactlyInAnyOrder("ray florets", "composite inflorescence");
    }

    @Test
    void forRankName_returnsOnlyTheGenusRankFeature_notTheFamilyOnes() {
        // Seeded GENUS-rank assignment for helianthus: "ray florets" only — the family
        // (asteraceae) also carries "composite inflorescence", but that must not leak
        // into the genus-scoped result since forRankName is direct-rank only.
        var result = featureQuery.forRankName(TestPlantsIdentifiers.PlantGenera.Helianthus.name);

        assertThat(result.stream().map(PlantFeature::value))
                .containsExactly("ray florets");
    }

    @Test
    void forRankName_noAssignments_returnsEmpty() {
        // Piperales carries no seeded feature assignments at all.
        var result = featureQuery.forRankName(TestPlantsIdentifiers.PlantOrders.Piperales.name);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void forRankName_rejectsNull() {
        assertThat(catchThrowable(() -> featureQuery.forRankName(null)))
                .isInstanceOf(com.naturalist.exception.InvariantViolationException.class);
    }
}
