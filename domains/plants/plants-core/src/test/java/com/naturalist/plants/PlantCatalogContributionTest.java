package com.naturalist.plants;

import com.naturalist.catalog.*;
import com.naturalist.catalog.CatalogContribution.SearchableEntity;
import com.naturalist.catalog.inmem.CatalogAssembly;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.plants.TestPlantsIdentifiers.Plants;
import com.naturalist.plants.catalog.PlantCatalogContribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lives in {@code com.naturalist.plants} (not {@code .catalog}) so the test can
 * see the package-private {@link PlantEntityQueryImpl} and the
 * protected-constructor {@link PlantEntityRepositoryMock} without exposing
 * either to the wider test classpath.
 */
class PlantCatalogContributionTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    private final PlantRepository.PlantEntityRepository repository = new PlantEntityRepositoryMock(db);
    private final PlantQuery.PlantEntityQuery entityQuery = new PlantEntityQueryImpl(repository);
    private final PlantCatalogContribution contribution = new PlantCatalogContribution(entityQuery);

    @Test
    void domainIsPlants() {
        assertThat(contribution.domain()).isEqualTo(new PlantsDomain());
    }

    @Test
    void constructorRejectsNullEntityQuery() {
        assertThatThrownBy(() -> new PlantCatalogContribution(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("plants");
    }

    @Test
    void californiaPipevineIsReachableThroughSlugBinomialGenusAbbreviationAndCommonNames() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new PlantsDomain(), Plants.CaliforniaPipevine.name);

        // Slug — strongest match.
        assertThat(catalog.search("aristolochia-californica").stream())
                .anyMatch(h -> h.target().equals(expected) && h.kind() == MatchKind.EXACT_SLUG);

        // Full binomial — case-insensitive.
        assertThat(targetsOf(catalog.search("Aristolochia californica"))).contains(expected);
        assertThat(targetsOf(catalog.search("aristolochia californica"))).contains(expected);

        // Genus alone.
        assertThat(targetsOf(catalog.search("Aristolochia"))).contains(expected);

        // Abbreviated binomial — tokenises as "a" + "californica".
        assertThat(targetsOf(catalog.search("A. californica"))).contains(expected);

        // Common names harvested from the JSON catalog.
        assertThat(targetsOf(catalog.search("pipevine"))).contains(expected);
        assertThat(targetsOf(catalog.search("California Dutchman's pipe"))).contains(expected);
    }

    @Test
    void searchIsCaseInsensitive() {
        Catalog catalog = CatalogAssembly.from(contribution);

        assertThat(catalog.search("ARISTOLOCHIA").size())
                .isEqualTo(catalog.search("aristolochia").size());
        assertThat(catalog.search("ARISTOLOCHIA-CALIFORNICA").size())
                .isEqualTo(catalog.search("aristolochia-californica").size());
    }

    @Test
    void unknownTokenReturnsEmptyResults() {
        Catalog catalog = CatalogAssembly.from(contribution);

        assertThat(catalog.search("not-a-plant-anywhere").isEmpty()).isTrue();
        assertThat(catalog.search("zzzzzzz").isEmpty()).isTrue();
    }

    @Test
    void genusTokenReturnsBothTrifoliumSpecies() {
        // The catalog includes two Trifolium species (trifolium-incarnatum
        // and trifolium-repens). Under search-and-discovery this is a feature,
        // not a collision — both species surface, and the reader picks.
        Catalog catalog = CatalogAssembly.from(contribution);

        Set<PlantName> trifoliumHits = catalog.search("Trifolium").stream()
                .map(SearchHit::target)
                .map(EntityRef::name)
                .map(name -> (PlantName) name)
                .collect(Collectors.toSet());

        assertThat(trifoliumHits).contains(
                PlantName.of("trifolium-incarnatum"),
                PlantName.of("trifolium-repens"));
    }

    @Test
    void commonNameSearchHitsBothCloverSpeciesIndependently() {
        Catalog catalog = CatalogAssembly.from(contribution);

        // trifolium-incarnatum carries "Crimson clover" / "Italian clover" —
        // "italian" disambiguates from white clover.
        assertThat(targetsOf(catalog.search("Italian clover")))
                .map(EntityRef::name)
                .contains(PlantName.of("trifolium-incarnatum"));

        // trifolium-repens carries "Dutch clover" / "Ladino clover" — both
        // unique to the white-clover entry.
        assertThat(targetsOf(catalog.search("Ladino clover")))
                .map(EntityRef::name)
                .contains(PlantName.of("trifolium-repens"));
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
                assertThat(entity.target().domain()).isEqualTo(new PlantsDomain()));
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
