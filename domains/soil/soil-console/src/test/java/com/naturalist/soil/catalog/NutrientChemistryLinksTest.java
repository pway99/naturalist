package com.naturalist.soil.catalog;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.catalog.SearchResults;
import com.naturalist.chemistry.element.ElementName;
import com.naturalist.ddd.EntityName;
import com.naturalist.soil.observation.NutrientName;
import com.naturalist.soil.observation.Nutrients;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NutrientChemistryLinksTest {

    private record TestDomain() implements DomainId {
        @Override
        public String value() {
            return "chemistry";
        }
    }

    /** Owns exactly the slugs it is given; everything else is unknown. */
    private record StubCatalog(Set<String> knownSlugs) implements Catalog {
        @Override
        public SearchResults search(String text) {
            return SearchResults.empty();
        }

        @Override
        public Set<DomainId> domainsReferencing(Class<? extends EntityName> referenceType) {
            return Set.of();
        }

        @Override
        public Map<DomainId, List<EntityRef>> findReferencesTo(EntityName target) {
            return Map.of();
        }

        @Override
        public Optional<EntityRef> findBySlug(String slug) {
            return knownSlugs.contains(slug)
                    ? Optional.of(new EntityRef(new TestDomain(), ElementName.of(slug)))
                    : Optional.empty();
        }
    }

    private static final EntityRefLinker LINKER = ref ->
            ref.name() instanceof ElementName n ? "/chemistry/elements/" + n.value() : null;

    private NutrientChemistryLinks links(String... knownSlugs) {
        return NutrientChemistryLinks.of(new StubCatalog(Set.of(knownSlugs)), LINKER);
    }

    @Test
    void linksBothCalciumFractionsToTheCalciumElement() {
        NutrientChemistryLinks links = links("calcium");

        assertThat(links.linkFor(Nutrients.CALCIUM_EXCHANGEABLE))
                .isEqualTo("/chemistry/elements/calcium");
        assertThat(links.linkFor(Nutrients.CALCIUM_SOLUBLE))
                .isEqualTo("/chemistry/elements/calcium");
    }

    @Test
    void linksAnOxideEquivalentToItsElement() {
        assertThat(links("phosphorus").linkFor(Nutrients.PHOSPHORUS_P2O5))
                .isEqualTo("/chemistry/elements/phosphorus");
    }

    @Test
    void returnsNullWhenTheCatalogOwnsNoSuchSlug() {
        // The element is declared by soil but absent from chemistry — plain text, no 404.
        assertThat(links("calcium").linkFor(Nutrients.BORON)).isNull();
    }

    @Test
    void returnsNullForANutrientWithNoDeclaredChemistry() {
        assertThat(links("calcium").linkFor(NutrientName.of("molybdenum-dtpa"))).isNull();
    }

    @Test
    void returnsNullWhenNoLinkerOwnsTheRef() {
        NutrientChemistryLinks links =
                NutrientChemistryLinks.of(new StubCatalog(Set.of("calcium")), ref -> null);

        assertThat(links.linkFor(Nutrients.CALCIUM_SOLUBLE)).isNull();
    }

    @Test
    void titleNamesTheSubstanceAndTheReportedForm() {
        assertThat(links("phosphorus").titleFor(Nutrients.PHOSPHORUS_P2O5))
                .isEqualTo("phosphorus — reported as an oxide equivalent");
        assertThat(links("sulfur").titleFor(Nutrients.SULFATE))
                .isEqualTo("sulfur — reported as an ion");
    }

    @Test
    void titleIsEmptyForANutrientWithNoDeclaredChemistry() {
        assertThat(links("calcium").titleFor(NutrientName.of("molybdenum-dtpa"))).isEmpty();
    }

    @Test
    void noneLinksNothing() {
        assertThat(NutrientChemistryLinks.none().linkFor(Nutrients.CALCIUM_SOLUBLE)).isNull();
        assertThat(NutrientChemistryLinks.none().titleFor(Nutrients.CALCIUM_SOLUBLE)).isEmpty();
    }
}
