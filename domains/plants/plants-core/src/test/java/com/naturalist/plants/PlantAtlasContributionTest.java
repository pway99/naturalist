package com.naturalist.plants;

import com.naturalist.atlas.Atlas;
import com.naturalist.atlas.AtlasAssembly;
import com.naturalist.atlas.DomainId;
import com.naturalist.atlas.EntityRef;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.plants.TestPlantsIdentifiers.Plants;
import com.naturalist.plants.atlas.PlantAtlasContribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lives in {@code com.naturalist.plants} (not {@code .atlas}) so the test can
 * see the package-private {@link PlantEntityQueryImpl} and the
 * protected-constructor {@link PlantEntityRepositoryMock} without exposing
 * either to the wider test classpath.
 */
class PlantAtlasContributionTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    private final PlantRepository.PlantEntityRepository repository = new PlantEntityRepositoryMock(db);
    private final PlantQuery.PlantEntityQuery entityQuery = new PlantEntityQueryImpl(repository);
    private final PlantAtlasContribution contribution = new PlantAtlasContribution(entityQuery);

    @Test
    void domainIsPlants() {
        assertThat(contribution.domain()).isEqualTo(new DomainId.Plants());
    }

    @Test
    void constructorRejectsNullEntityQuery() {
        assertThatThrownBy(() -> new PlantAtlasContribution(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("plants");
    }

    @Test
    void aliasesResolveCaliforniaPipevineThroughAllFourDerivedForms() {
        Atlas atlas = AtlasAssembly.from(contribution);
        EntityRef expected = new EntityRef(new DomainId.Plants(), Plants.CaliforniaPipevine.name);

        // Slug.
        assertThat(atlas.resolveAlias("california-pipevine")).contains(expected);
        // Genus.
        assertThat(atlas.resolveAlias("Aristolochia")).contains(expected);
        // Full binomial.
        assertThat(atlas.resolveAlias("Aristolochia californica")).contains(expected);
        // Abbreviated binomial.
        assertThat(atlas.resolveAlias("A. californica")).contains(expected);
    }

    @Test
    void aliasesAreCaseSensitive() {
        Atlas atlas = AtlasAssembly.from(contribution);

        assertThat(atlas.resolveAlias("aristolochia")).isEmpty();
        assertThat(atlas.resolveAlias("aristolochia californica")).isEmpty();
        assertThat(atlas.resolveAlias("California-Pipevine")).isEmpty();
    }

    @Test
    void unknownSurfaceFormReturnsEmpty() {
        Atlas atlas = AtlasAssembly.from(contribution);

        assertThat(atlas.resolveAlias("not-a-plant")).isEmpty();
        assertThat(atlas.resolveAlias("Battus philenor")).isEmpty();
    }

    @Test
    void ambiguousGenusIsDroppedNotConflicted() {
        // The catalog includes two Trifolium species (crimson-clover and
        // white-clover). The genus alone is therefore ambiguous and would
        // otherwise collide at assembly. The contribution silently drops
        // it; the binomials and slugs still resolve cleanly.
        Atlas atlas = AtlasAssembly.from(contribution);

        assertThat(atlas.resolveAlias("Trifolium")).isEmpty();
        assertThat(atlas.resolveAlias("Trifolium incarnatum"))
                .map(EntityRef::name)
                .contains(PlantName.of("crimson-clover"));
        assertThat(atlas.resolveAlias("crimson-clover"))
                .map(EntityRef::name)
                .contains(PlantName.of("crimson-clover"));
    }

    @Test
    void contributionEmitsAtLeastOneAliasPerPlant() {
        // Lower bound: every plant contributes at least its slug. Upper bound
        // is open because ambiguous genus aliases drop out.
        long plantCount = entityQuery.allPlantNames().size();
        long aliasCount = contribution.aliases().count();

        assertThat(aliasCount).isGreaterThanOrEqualTo(plantCount);
    }

    @Test
    void aliasesArePointedAtThePlantsDomain() {
        contribution.aliases().forEach(alias ->
                assertThat(alias.target().domain()).isEqualTo(new DomainId.Plants()));
    }
}
