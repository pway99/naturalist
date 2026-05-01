package com.naturalist.plants;

import com.naturalist.atlas.Atlas;
import com.naturalist.atlas.inmem.AtlasAssembly;
import com.naturalist.atlas.AtlasContribution.SearchableEntity;
import com.naturalist.atlas.DomainId;
import com.naturalist.atlas.EntityRef;
import com.naturalist.atlas.MatchKind;
import com.naturalist.atlas.SearchHit;
import com.naturalist.atlas.SearchResults;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.plants.TestPlantsIdentifiers.Plants;
import com.naturalist.plants.atlas.PlantAtlasContribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    void californiaPipevineIsReachableThroughSlugBinomialGenusAbbreviationAndCommonNames() {
        Atlas atlas = AtlasAssembly.from(contribution);
        EntityRef expected = new EntityRef(new DomainId.Plants(), Plants.CaliforniaPipevine.name);

        // Slug — strongest match.
        assertThat(atlas.search("california-pipevine").stream())
                .anyMatch(h -> h.target().equals(expected) && h.kind() == MatchKind.EXACT_SLUG);

        // Full binomial — case-insensitive.
        assertThat(targetsOf(atlas.search("Aristolochia californica"))).contains(expected);
        assertThat(targetsOf(atlas.search("aristolochia californica"))).contains(expected);

        // Genus alone.
        assertThat(targetsOf(atlas.search("Aristolochia"))).contains(expected);

        // Abbreviated binomial — tokenises as "a" + "californica".
        assertThat(targetsOf(atlas.search("A. californica"))).contains(expected);

        // Common names harvested from the JSON catalog.
        assertThat(targetsOf(atlas.search("pipevine"))).contains(expected);
        assertThat(targetsOf(atlas.search("California Dutchman's pipe"))).contains(expected);
    }

    @Test
    void searchIsCaseInsensitive() {
        Atlas atlas = AtlasAssembly.from(contribution);

        assertThat(atlas.search("ARISTOLOCHIA").size())
                .isEqualTo(atlas.search("aristolochia").size());
        assertThat(atlas.search("CALIFORNIA-PIPEVINE").size())
                .isEqualTo(atlas.search("california-pipevine").size());
    }

    @Test
    void unknownTokenReturnsEmptyResults() {
        Atlas atlas = AtlasAssembly.from(contribution);

        assertThat(atlas.search("not-a-plant-anywhere").isEmpty()).isTrue();
        assertThat(atlas.search("zzzzzzz").isEmpty()).isTrue();
    }

    @Test
    void genusTokenReturnsBothTrifoliumSpecies() {
        // The catalog includes two Trifolium species (crimson-clover and
        // white-clover). Under search-and-discovery this is a feature, not a
        // collision — both species surface, and the reader picks.
        Atlas atlas = AtlasAssembly.from(contribution);

        Set<PlantName> trifoliumHits = atlas.search("Trifolium").stream()
                .map(SearchHit::target)
                .map(EntityRef::name)
                .map(name -> (PlantName) name)
                .collect(Collectors.toSet());

        assertThat(trifoliumHits).contains(
                PlantName.of("crimson-clover"),
                PlantName.of("white-clover"));
    }

    @Test
    void commonNameSearchHitsBothCloverSpeciesIndependently() {
        Atlas atlas = AtlasAssembly.from(contribution);

        // Crimson clover has "Crimson clover" / "Italian clover" — "italian"
        // disambiguates from white clover.
        assertThat(targetsOf(atlas.search("Italian clover")))
                .map(EntityRef::name)
                .contains(PlantName.of("crimson-clover"));

        // White clover has "Dutch clover" / "Ladino clover" — both unique to
        // the white-clover entry.
        assertThat(targetsOf(atlas.search("Ladino clover")))
                .map(EntityRef::name)
                .contains(PlantName.of("white-clover"));
    }

    @Test
    void contributionEmitsOneSearchableEntityPerPlant() {
        long plantCount = entityQuery.allPlantNames().size();
        long entityCount = contribution.searchableEntities().count();

        assertThat(entityCount).isEqualTo(plantCount);
    }

    @Test
    void everySearchableEntityIsAttributedToThePlantsDomain() {
        contribution.searchableEntities().forEach(entity ->
                assertThat(entity.target().domain()).isEqualTo(new DomainId.Plants()));
    }

    @Test
    void plantWithoutSpeciesContributesGenusButNoBinomial() {
        // creeping-thyme has genus "Thymus" with null species. The token
        // stream should include the slug and the genus, and skip the binomial
        // forms — no NullPointerException, no malformed token.
        SearchableEntity creepingThyme = contribution.searchableEntities()
                .filter(e -> e.target().name().equals(PlantName.of("creeping-thyme")))
                .findFirst()
                .orElseThrow();
        List<String> tokens = creepingThyme.tokens().toList();

        assertThat(tokens).contains("creeping-thyme", "Thymus");
        assertThat(tokens).noneMatch(t -> t.contains(" ") && t.startsWith("Thymus "));
        assertThat(tokens).noneMatch(t -> t.startsWith("T. "));
    }

    private static List<EntityRef> targetsOf(SearchResults results) {
        return results.stream().map(SearchHit::target).toList();
    }
}
