package com.naturalist.insects;

import com.naturalist.catalog.*;
import com.naturalist.catalog.CatalogContribution.SearchableEntity;
import com.naturalist.catalog.inmem.CatalogAssembly;
import com.naturalist.data.NaturalistDatabaseExtension;
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

    private final SpeciesRepositoryMock repository = new SpeciesRepositoryMock(db);
    private final InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(repository);
    private final InsectsCatalogContribution contribution = new InsectsCatalogContribution(speciesQuery);

    @Test
    void domainIsInsects() {
        assertThat(contribution.domain()).isEqualTo(new InsectsDomain());
    }

    @Test
    void constructorRejectsNullEntityQuery() {
        assertThatThrownBy(() -> new InsectsCatalogContribution(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("species");
    }

    @Test
    void contributionEmitsOneSearchableEntityPerSpecies() {
        long speciesCount = speciesQuery.allSpeciesNames().size();
        long entityCount = contribution.searchableEntities().count();

        assertThat(entityCount).isEqualTo(speciesCount);
    }

    @Test
    void everySearchableEntityIsAttributedToTheInsectsDomain() {
        contribution.searchableEntities().forEach(entity ->
                assertThat(entity.target().domain()).isEqualTo(new InsectsDomain()));
    }

    @Test
    void convergentLadybugIsReachableThroughSlugBinomialGenusAndAbbreviation() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(
                new InsectsDomain(),
                InsectSpeciesName.of("convergent-ladybug"));

        // Slug — strongest match.
        assertThat(catalog.search("convergent-ladybug").stream())
                .as("slug 'convergent-ladybug' should resolve to %s as EXACT_SLUG", expected)
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
    }

    @Test
    void searchIsCaseInsensitive() {
        Catalog catalog = CatalogAssembly.from(contribution);

        assertThat(catalog.search("HIPPODAMIA").size())
                .isEqualTo(catalog.search("hippodamia").size());
        assertThat(catalog.search("CONVERGENT-LADYBUG").size())
                .isEqualTo(catalog.search("convergent-ladybug").size());
    }

    @Test
    void unknownTokenReturnsEmptyResults() {
        Catalog catalog = CatalogAssembly.from(contribution);
        assertThat(catalog.search("zzzzzzz").isEmpty()).isTrue();
    }

    @Test
    void speciesWithoutGenusContributesSlugButNoBinomial() {
        // tachinid-fly is catalogued at family level (Tachinidae) — both
        // genus and species are null. The token stream should include the
        // slug only and skip every binomial form — no NullPointerException,
        // no malformed token.
        SearchableEntity tachinidFly = contribution.searchableEntities()
                .filter(e -> e.target().name().equals(InsectSpeciesName.of("tachinid-fly")))
                .findFirst()
                .orElseThrow();
        List<String> tokens = tachinidFly.tokens().toList();

        assertThat(tokens).containsExactly("tachinid-fly");
    }

    private static List<EntityRef> targetsOf(SearchResults results) {
        return results.stream().map(SearchHit::target).toList();
    }
}
