package com.naturalist.console.catalog;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.soil.observation.NutrientChemistry;
import com.naturalist.soil.observation.NutrientName;
import com.naturalist.soil.observation.Nutrients;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the composed nutrient -> catalog -> linker seam that the rest of the
 * suite only exercises in isolation. {@code NutrientChemistryLinksTest} (in
 * {@code soil-console}) proves the wiring shape against a {@code StubCatalog}
 * and a lambda linker; {@code ChemistryCatalogContributionTest} proves the
 * real catalog resolves individual element slugs; neither proves that every
 * substance {@link Nutrients} actually names resolves through the real,
 * assembled {@link Catalog} to a real chemistry element URL via the real
 * {@link EntityRefLinker} composite. This is the only module that depends on
 * both soil and chemistry plus the assembled catalog, so it is the only place
 * this can be proven.
 * <p>
 * A typo'd or newly added substance slug in {@link Nutrients#CHEMISTRY} fails
 * here even though every other test in the suite stays green.
 */
@SpringBootTest
class NutrientChemistryCatalogLinkingTest {

    @Autowired
    Catalog catalog;

    @Autowired
    EntityRefLinker linker;

    @Test
    void everyCataloguedNutrientResolvesToAChemistryElementUrl() {
        for (NutrientName nutrient : Nutrients.ALL) {
            NutrientChemistry chemistry = Nutrients.chemistryOf(nutrient)
                    .orElseThrow(() -> new AssertionError(
                            "nutrient '" + nutrient.value() + "' has no declared chemistry"));

            EntityRef ref = catalog.findBySlug(chemistry.substance().value())
                    .orElseThrow(() -> new AssertionError(
                            "catalog has no entity for slug '" + chemistry.substance().value()
                                    + "' referenced by nutrient '" + nutrient.value() + "'"));

            String url = linker.linkFor(ref);

            assertThat(url)
                    .as("nutrient '%s' (substance '%s') must resolve to a chemistry element URL",
                            nutrient.value(), chemistry.substance().value())
                    .isNotNull()
                    .startsWith("/chemistry/elements/");
        }
    }
}
