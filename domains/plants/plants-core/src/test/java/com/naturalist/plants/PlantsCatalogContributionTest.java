package com.naturalist.plants;

import com.naturalist.catalog.*;
import com.naturalist.catalog.CatalogContribution.SearchableEntity;
import com.naturalist.catalog.inmem.CatalogAssembly;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.Pages;
import com.naturalist.ddd.EntityName;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.plants.TestPlantsIdentifiers.Plants;
import com.naturalist.plants.catalog.PlantsCatalogContribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lives in {@code com.naturalist.plants} (not {@code .catalog}) so the test can
 * see the package-private {@link PlantSpeciesQueryImpl} and the package-private
 * {@link PlantSpeciesRepositoryMock} without exposing either to the wider test
 * classpath.
 */
class PlantsCatalogContributionTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    private final PlantQuery.GenusQuery genusQuery =
            new PlantGenusQueryImpl(new PlantGenusRepositoryMock(nte));
    private final PlantQuery.SpeciesQuery entityQuery =
            new PlantSpeciesQueryImpl(new PlantSpeciesRepositoryMock(nte), genusQuery);
    private final PlantQuery.OrderQuery orderQuery =
            new PlantOrderQueryImpl(new PlantOrderRepositoryMock(nte));
    private final PlantQuery.FamilyQuery familyQuery =
            new PlantFamilyQueryImpl(new PlantFamilyRepositoryMock(nte));
    private final PlantsCatalogContribution contribution =
            new PlantsCatalogContribution(entityQuery, orderQuery, familyQuery, genusQuery);

    @Test
    void domainIsPlants() {
        assertThat(contribution.domain()).isEqualTo(new PlantsDomain());
    }

    @Test
    void constructorRejectsNullPlantQuery() {
        assertThatThrownBy(() -> new PlantsCatalogContribution(null, orderQuery, familyQuery, genusQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("plants");
    }

    @Test
    void constructorRejectsNullOrderQuery() {
        assertThatThrownBy(() -> new PlantsCatalogContribution(entityQuery, null, familyQuery, genusQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("orders");
    }

    @Test
    void constructorRejectsNullFamilyQuery() {
        assertThatThrownBy(() -> new PlantsCatalogContribution(entityQuery, orderQuery, null, genusQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("families");
    }

    @Test
    void constructorRejectsNullGenusQuery() {
        assertThatThrownBy(() -> new PlantsCatalogContribution(entityQuery, orderQuery, familyQuery, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("genera");
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

        assertThat(catalog.search("qwzxv-unobtainium-flarble").isEmpty()).isTrue();
        assertThat(catalog.search("zzzzzzz").isEmpty()).isTrue();
    }

    @Test
    void genusTokenReturnsBothTrifoliumSpeciesAndTheGenusRecord() {
        // Two Trifolium species (trifolium-incarnatum, trifolium-repens) emit
        // "Trifolium" as a genus token, and since the genus catalog was
        // backfilled the trifolium PlantGenus record emits it too. Under
        // search-and-discovery all three are wanted — the reader picks the rank
        // they meant, and every one of them now resolves to a page.
        //
        // Asserted on the raw EntityName, never cast: the hits are deliberately
        // of mixed name type. EntityName equality is class-qualified, so a
        // PlantGenusName holding "trifolium" never equals a PlantSpeciesName holding
        // the same string — contains() is exact without a cast.
        Catalog catalog = CatalogAssembly.from(contribution);

        Set<EntityName> trifoliumHits = catalog.search("Trifolium").stream()
                .map(SearchHit::target)
                .map(EntityRef::name)
                .collect(Collectors.toSet());

        assertThat(trifoliumHits).contains(
                PlantSpeciesName.of("trifolium-incarnatum"),
                PlantSpeciesName.of("trifolium-repens"),
                PlantGenusName.of("trifolium"));
    }

    @Test
    void commonNameSearchHitsBothCloverSpeciesIndependently() {
        Catalog catalog = CatalogAssembly.from(contribution);

        // trifolium-incarnatum carries "Crimson clover" / "Italian clover" —
        // "italian" disambiguates from white clover.
        assertThat(targetsOf(catalog.search("Italian clover")))
                .map(EntityRef::name)
                .contains(PlantSpeciesName.of("trifolium-incarnatum"));

        // trifolium-repens carries "Dutch clover" / "Ladino clover" — both
        // unique to the white-clover entry.
        assertThat(targetsOf(catalog.search("Ladino clover")))
                .map(EntityRef::name)
                .contains(PlantSpeciesName.of("trifolium-repens"));
    }

    @Test
    void contributionEmitsOneSearchableEntityPerCatalogEntry() {
        long plantCount = Pages.stream(1000, entityQuery::findPage).count();
        long orderCount = Pages.stream(1000, orderQuery::findPage).count();
        long familyCount = Pages.stream(1000, familyQuery::findPage).count();
        long genusCount = Pages.stream(1000, genusQuery::findPage).count();
        long entityCount = contribution.searchableEntities().count();

        assertThat(entityCount)
                .isEqualTo(plantCount + orderCount + familyCount + genusCount);
    }

    @Test
    void familiesAreSearchableBySlugAndEpithet() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new PlantsDomain(), PlantFamilyName.of("aristolochiaceae"));

        assertThat(catalog.search("aristolochiaceae").stream())
                .anyMatch(h -> h.target().equals(expected) && h.kind() == MatchKind.EXACT_SLUG);
        assertThat(targetsOf(catalog.search("Aristolochiaceae"))).contains(expected);
    }

    @Test
    void ordersAreSearchableBySlugAndEpithet() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new PlantsDomain(), PlantOrderName.of("lamiales"));

        assertThat(catalog.search("lamiales").stream())
                .anyMatch(h -> h.target().equals(expected) && h.kind() == MatchKind.EXACT_SLUG);
        assertThat(targetsOf(catalog.search("Lamiales"))).contains(expected);
    }

    @Test
    void generaAreSearchableBySlugAndEpithet() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new PlantsDomain(), PlantGenusName.of("thymus"));

        assertThat(catalog.search("thymus").stream())
                .anyMatch(h -> h.target().equals(expected) && h.kind() == MatchKind.EXACT_SLUG);
        assertThat(targetsOf(catalog.search("Thymus"))).contains(expected);
    }

    @Test
    void everySearchableEntityIsAttributedToThePlantsDomain() {
        contribution.searchableEntities().forEach(entity ->
                assertThat(entity.target().domain()).isEqualTo(new PlantsDomain()));
    }

    private static List<EntityRef> targetsOf(SearchResults results) {
        return results.stream().map(SearchHit::target).toList();
    }
}
