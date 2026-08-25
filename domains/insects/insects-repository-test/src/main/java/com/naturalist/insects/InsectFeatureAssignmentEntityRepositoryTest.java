package com.naturalist.insects;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.taxonomy.OrganismFeatureAssignment;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link InsectRepository.FeatureAssignmentRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002). Supplies
 * {@code InsectFeatureAssignment}-specific identity constants and entity
 * construction. The {@code newEntity} hook assigns an existing feature to a
 * rank that does not yet carry an assignment for that feature.
 */
interface InsectFeatureAssignmentEntityRepositoryTest
        extends EntityRepositoryTest<InsectFeatureAssignmentId,
                OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> {

    @Override
    InsectRepository.FeatureAssignmentRepository repository();

    @Override
    default TestEntitySource<InsectFeatureAssignmentId,
            OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> source() {
        return db.getNamed(InsectFeatureAssignmentTestEntitySource.class);
    }

    @Override
    default InsectFeatureAssignmentId notFoundName() {
        return TestInsectsIdentifiers.InsectFeatureAssignment.NotFound.id;
    }

    @Override
    default List<InsectFeatureAssignmentId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectFeatureAssignment.CompleteMetamorphosisDiptera.id,
                TestInsectsIdentifiers.InsectFeatureAssignment.CompleteMetamorphosisLepidoptera.id,
                TestInsectsIdentifiers.InsectFeatureAssignment.ScaledWingsLepidoptera.id);
    }

    @Override
    default OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName> newEntity() {
        // "complete metamorphosis" assigned to neuroptera — feature exists (FK passes),
        // neuroptera order exists, and the (featureId, rankName) pair is not yet assigned.
        return OrganismFeatureAssignment.of(
                InsectFeatureAssignmentId.create(),
                TestInsectsIdentifiers.InsectFeature.CompleteMetamorphosis.id,
                InsectOrderName.of("neuroptera"),
                1);
    }

    @Override
    default OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName> ghostEntity() {
        return OrganismFeatureAssignment.of(
                InsectFeatureAssignmentId.create(),
                TestInsectsIdentifiers.InsectFeature.CompleteMetamorphosis.id,
                InsectOrderName.of("neuroptera"),
                1);
    }

    @Override
    default OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName> modifiedEntity(
            OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName> original) {
        // Change feature to "scaled wings" and rank to blattodea — both exist in catalogs,
        // and neither pair is currently assigned.
        return OrganismFeatureAssignment.of(
                original.id(),
                TestInsectsIdentifiers.InsectFeature.ScaledWings.id,
                TestInsectsIdentifiers.InsectOrder.Blattodea.name,
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
                InsectOrderName.of("diptera"), InsectOrderName.of("lepidoptera")));
        assertThat(result).isNotEmpty();
        assertThat(result).extracting(a -> a.rankName().value())
                .contains("diptera", "lepidoptera")
                .doesNotContain("coleoptera");
        // sanity: every returned assignment is in the requested set
        assertThat(result).allSatisfy(a ->
                assertThat(a.rankName().value()).isIn("diptera", "lepidoptera"));
    }

    @Test
    default void getByFeatureId_rejectsNull() {
        assertThatThrownBy(() -> repository().getByFeatureId(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("featureId");
    }
}
