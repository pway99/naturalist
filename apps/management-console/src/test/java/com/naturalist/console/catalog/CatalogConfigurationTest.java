package com.naturalist.console.catalog;

import com.naturalist.catalog.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the wiring assembles a working {@link Catalog} from
 * {@code @DomainService}-annotated beans alone — no per-domain wiring
 * code in {@link CatalogConfiguration}.
 *
 * <p>The "Aristolochia californica" resolution exercises the full plants
 * pilot chain: {@code PlantsDomain} (DomainId) →
 * {@code PlantCatalogContribution} (CatalogContribution) →
 * {@code PlantEntityQueryImpl} → {@code PlantEntityRepositoryMock} →
 * {@code NaturalistDatabase}. A regression in any link breaks
 * {@link #resolvesPlantBySlug()} or {@link #resolvesPlantByGenusToken()}.
 *
 * <p>The discovery assertions ({@link #discoversEveryDomainSubtype()},
 * {@link #discoversEveryDomainLinker()}) cover M7's broadened scan
 * scope: chemistry and insects ship only a {@link DomainId} subtype and
 * an {@link EntityRefLinker} today (no contribution, no provider), and
 * those minimal beans must still be picked up by
 * {@code DomainServiceScan} now that it covers the full
 * {@code com.naturalist} root. A new domain landing tomorrow with the
 * same minimal shape is automatically covered without an edit here.
 */
@SpringBootTest
class CatalogConfigurationTest {

    @Autowired
    Catalog catalog;

    @Autowired
    List<DomainId> domains;

    @Autowired
    List<EntityRefLinker> linkers;

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

    @Test
    void discoversEveryDomainSubtype() {
        assertThat(domains)
                .extracting(DomainId::value)
                .contains("plants", "chemistry", "insects");
    }

    @Test
    void discoversEveryDomainLinker() {
        assertThat(linkers)
                .extracting(linker -> linker.getClass().getSimpleName())
                .contains("PlantsLinker", "ChemistryLinker", "InsectsLinker");
    }
}
