package com.naturalist.plants;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link PlantRepository.FeatureAssignmentRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002). Supplies
 * {@code PlantFeatureAssignment}-specific identity constants and entity
 * construction. The {@code newEntity} hook assigns an existing feature to a
 * rank that does not yet carry an assignment for that feature. Mirrors
 * {@code InsectFeatureAssignmentEntityRepositoryTest}.
 */
interface PlantFeatureAssignmentEntityRepositoryTest
        extends EntityRepositoryTest<PlantFeatureAssignmentId, PlantFeatureAssignment> {

    @Override
    PlantRepository.FeatureAssignmentRepository repository();

    @Override
    default TestEntitySource<PlantFeatureAssignmentId, PlantFeatureAssignment> source() {
        return db.getNamed(PlantFeatureAssignmentTestEntitySource.class);
    }

    @Override
    default PlantFeatureAssignmentId notFoundName() {
        return TestPlantsIdentifiers.PlantFeatureAssignments.NotFound.id;
    }

    @Override
    default List<PlantFeatureAssignmentId> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.PlantFeatureAssignments.RayFloretsAsteraceae,
                TestPlantsIdentifiers.PlantFeatureAssignments.OppositeLeavesLamiaceae);
    }

    @Override
    default PlantFeatureAssignment newEntity() {
        // "ray florets" assigned to fabaceae — feature exists (FK passes), family
        // exists, and the (featureId, rankName) pair is not yet assigned.
        return new PlantFeatureAssignment(
                PlantFeatureAssignmentId.create(),
                TestPlantsIdentifiers.PlantFeatures.RayFlorets,
                TestPlantsIdentifiers.PlantFamilies.Fabaceae.name,
                1);
    }

    @Override
    default PlantFeatureAssignment ghostEntity() {
        return new PlantFeatureAssignment(
                PlantFeatureAssignmentId.create(),
                TestPlantsIdentifiers.PlantFeatures.RayFlorets,
                TestPlantsIdentifiers.PlantFamilies.Fabaceae.name,
                1);
    }

    @Override
    default PlantFeatureAssignment modifiedEntity(PlantFeatureAssignment original) {
        // Change feature to "opposite leaves" and rank to Rosaceae — both exist in
        // catalogs, and neither pair is currently assigned.
        return new PlantFeatureAssignment(
                original.id(),
                TestPlantsIdentifiers.PlantFeatures.OppositeLeaves,
                TestPlantsIdentifiers.PlantFamilies.Rosaceae.name,
                99);
    }

    @Test
    default void getByRankName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByRankName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("rankName");
    }

    @Test
    default void getByRankNames_rejectsNull() {
        assertThatThrownBy(() -> repository().getByRankNames(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("rankNames");
    }

    @Test
    default void getByRankNames_returnsAssignmentsAcrossTheGivenRanks() {
        var result = repository().getByRankNames(Set.of(
                TestPlantsIdentifiers.PlantFamilies.Asteraceae.name,
                TestPlantsIdentifiers.PlantFamilies.Lamiaceae.name));
        assertThat(result).isNotEmpty();
        assertThat(result).extracting(a -> a.rankName().value())
                .contains("asteraceae", "lamiaceae")
                .doesNotContain("fabaceae");
        // sanity: every returned assignment is in the requested set
        assertThat(result).allSatisfy(a ->
                assertThat(a.rankName().value()).isIn("asteraceae", "lamiaceae"));
    }

    @Test
    default void getByFeatureId_rejectsNull() {
        assertThatThrownBy(() -> repository().getByFeatureId(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("featureId");
    }
}
