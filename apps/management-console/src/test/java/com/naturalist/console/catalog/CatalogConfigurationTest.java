package com.naturalist.console.catalog;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.MatchKind;
import com.naturalist.catalog.SearchResults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the M6 wiring assembles a working {@link Catalog} from
 * {@code @DomainService}-annotated beans alone — no per-domain wiring
 * code in {@link CatalogConfiguration}.
 *
 * <p>The "Aristolochia californica" resolution exercises the full plants
 * pilot chain: {@code PlantsDomain} (DomainId) →
 * {@code PlantCatalogContribution} (CatalogContribution) →
 * {@code PlantEntityQueryImpl} → {@code PlantEntityRepositoryMock} →
 * {@code NaturalistDatabase}. A regression in any link breaks this test.
 */
@SpringBootTest
class CatalogConfigurationTest {

    @Autowired
    Catalog catalog;

    @Test
    void resolvesPlantBySlug() {
        SearchResults results = catalog.search("california-pipevine");
        assertThat(results.stream())
                .anySatisfy(hit -> {
                    assertThat(hit.target().name().value()).isEqualTo("california-pipevine");
                    assertThat(hit.kind()).isEqualTo(MatchKind.EXACT_SLUG);
                });
    }

    @Test
    void resolvesPlantByGenusToken() {
        SearchResults results = catalog.search("Aristolochia");
        assertThat(results.stream())
                .anySatisfy(hit ->
                        assertThat(hit.target().name().value()).isEqualTo("california-pipevine"));
    }
}
