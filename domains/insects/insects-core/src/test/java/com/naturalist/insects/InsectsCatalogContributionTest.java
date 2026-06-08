package com.naturalist.insects;

import com.naturalist.catalog.*;
import com.naturalist.catalog.inmem.CatalogAssembly;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.data.Pages;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.catalog.InsectsCatalogContribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lives in {@code com.naturalist.insects} (not {@code .catalog}) so the test
 * can see the package-private {@link SpeciesQueryImpl} and the
 * package-private {@link SpeciesRepositoryMock} without exposing either to
 * the wider test classpath.
 */
class InsectsCatalogContributionTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    private final InsectQuery.FamilyQuery familyQuery =
            new FamilyQueryImpl(new FamilyRepositoryMock(db));
    private final InsectQuery.GenusQuery genusQuery =
            new GenusQueryImpl(new GenusRepositoryMock(db), familyQuery);
    private final InsectQuery.SpeciesQuery speciesQuery =
            new SpeciesQueryImpl(new SpeciesRepositoryMock(db), genusQuery);
    private final InsectQuery.OrderQuery orderQuery =
            new OrderQueryImpl(new OrderRepositoryMock(db));
    private final InsectsCatalogContribution contribution =
            new InsectsCatalogContribution(speciesQuery, familyQuery, genusQuery, orderQuery);

    @Test
    void domainIsInsects() {
        assertThat(contribution.domain()).isEqualTo(new InsectsDomain());
    }

    @Test
    void constructorRejectsNullSpeciesQuery() {
        assertThatThrownBy(() -> new InsectsCatalogContribution(null, familyQuery, genusQuery, orderQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("species");
    }

    @Test
    void constructorRejectsNullFamilyQuery() {
        assertThatThrownBy(() -> new InsectsCatalogContribution(speciesQuery, null, genusQuery, orderQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("families");
    }

    @Test
    void constructorRejectsNullGenusQuery() {
        assertThatThrownBy(() -> new InsectsCatalogContribution(speciesQuery, familyQuery, null, orderQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("genera");
    }

    @Test
    void constructorRejectsNullOrderQuery() {
        assertThatThrownBy(() -> new InsectsCatalogContribution(speciesQuery, familyQuery, genusQuery, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("orders");
    }

    @Test
    void contributionEmitsOneSearchableEntityPerCatalogEntry() {
        long speciesCount = Pages.stream(1000, speciesQuery::findPage).count();
        long familyCount = Pages.stream(1000, familyQuery::findPage).count();
        long genusCount = Pages.stream(1000, genusQuery::findPage).count();
        long orderCount = Pages.stream(1000, orderQuery::findPage).count();
        long entityCount = contribution.searchableEntities().count();

        assertThat(entityCount).isEqualTo(speciesCount + familyCount + genusCount + orderCount);
    }

    @Test
    void familiesAreSearchableBySlugAndEpithet() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new InsectsDomain(), InsectFamilyName.of("halictidae"));

        assertThat(catalog.search("halictidae").stream())
                .anyMatch(h -> h.target().equals(expected) && h.kind() == MatchKind.EXACT_SLUG);
        assertThat(targetsOf(catalog.search("Halictidae"))).contains(expected);
    }

    @Test
    void generaAreSearchableBySlugAndEpithet() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new InsectsDomain(), InsectGenusName.of("halictus"));

        assertThat(catalog.search("halictus").stream())
                .anyMatch(h -> h.target().equals(expected) && h.kind() == MatchKind.EXACT_SLUG);
        assertThat(targetsOf(catalog.search("Halictus"))).contains(expected);
    }

    @Test
    void everySearchableEntityIsAttributedToTheInsectsDomain() {
        contribution.searchableEntities().forEach(entity ->
                assertThat(entity.target().domain()).isEqualTo(new InsectsDomain()));
    }

    @Test
    void convergentLadybugIsReachableThroughSlugBinomialGenusAbbreviationAndCommonName() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(
                new InsectsDomain(),
                InsectSpeciesName.of("hippodamia-convergens"));

        // Slug — strongest match.
        assertThat(catalog.search("hippodamia-convergens").stream())
                .as("slug 'hippodamia-convergens' should resolve to %s as EXACT_SLUG", expected)
                .anyMatch(h -> h.target().equals(expected) && h.kind() == MatchKind.EXACT_SLUG);

        // Full binomial — case-insensitive.
        assertThat(targetsOf(catalog.search("Hippodamia convergens")))
                .as("binomial 'Hippodamia convergens' should resolve to %s", expected)
                .contains(expected);
        assertThat(targetsOf(catalog.search("hippodamia convergens")))
                .as("lowercase binomial 'hippodamia convergens' should resolve to %s", expected)
                .contains(expected);

        // Genus alone.
        assertThat(targetsOf(catalog.search("Hippodamia")))
                .as("genus 'Hippodamia' should resolve to %s", expected)
                .contains(expected);

        // Abbreviated binomial — tokenises as "h" + "convergens".
        assertThat(targetsOf(catalog.search("H. convergens")))
                .as("abbreviated binomial 'H. convergens' should resolve to %s", expected)
                .contains(expected);

        // Common name harvested from the JSON catalog — the binomial slug
        // commitment makes vernacular search load-bearing rather than
        // duplicative.
        assertThat(targetsOf(catalog.search("Convergent Ladybug")))
                .as("common name 'Convergent Ladybug' should resolve to %s", expected)
                .contains(expected);
    }

    @Test
    void searchIsCaseInsensitive() {
        Catalog catalog = CatalogAssembly.from(contribution);

        assertThat(catalog.search("HIPPODAMIA").size())
                .isEqualTo(catalog.search("hippodamia").size());
        assertThat(catalog.search("HIPPODAMIA-CONVERGENS").size())
                .isEqualTo(catalog.search("hippodamia-convergens").size());
    }

    @Test
    void unknownTokenReturnsEmptyResults() {
        Catalog catalog = CatalogAssembly.from(contribution);
        assertThat(catalog.search("zzzzzzz").isEmpty()).isTrue();
    }

    @Test
    void ordersAreSearchableBySlugAndEpithet() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new InsectsDomain(), InsectOrderName.of("diptera"));

        assertThat(catalog.search("diptera").stream())
                .anyMatch(h -> h.target().equals(expected) && h.kind() == MatchKind.EXACT_SLUG);
        assertThat(targetsOf(catalog.search("Diptera"))).contains(expected);
    }

    private static List<EntityRef> targetsOf(SearchResults results) {
        return results.stream().map(SearchHit::target).toList();
    }
}
