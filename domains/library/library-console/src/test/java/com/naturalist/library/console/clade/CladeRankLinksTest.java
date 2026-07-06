package com.naturalist.library.console.clade;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.catalog.SearchResults;
import com.naturalist.ddd.EntityName;
import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CladeRankLinksTest {

    private record TestDomain() implements DomainId {
        @Override
        public String value() {
            return "insects";
        }
    }

    private static final class TestName extends EntityName {
        private TestName(String value) {
            super(value);
        }

        @Override
        protected int maxLength() {
            return 100;
        }
    }

    private static EntityRef ref(String slug) {
        return new EntityRef(new TestDomain(), new TestName(slug));
    }

    /** Catalog stub that resolves only the given slugs to refs. */
    private static Catalog catalogWith(Map<String, EntityRef> bySlug) {
        return new Catalog() {
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
                return Optional.ofNullable(bySlug.get(slug));
            }
        };
    }

    /** Linker stub mapping a ref's slug to a URL, or null for unhandled types. */
    private static EntityRefLinker linkerWith(Map<String, String> urlBySlug) {
        return ref -> urlBySlug.get(ref.name().value());
    }

    @Test
    void insectaShortCircuitsToOrdersLanding() {
        String url = CladeRankLinks.forStep(
                catalogWith(Map.of()), linkerWith(Map.of()), "insecta", LinealRank.CLASS);

        assertThat(url).isEqualTo("/insects/orders");
    }

    @Test
    void resolvedFamilyLinksToItsCatalogPage() {
        Catalog catalog = catalogWith(Map.of("papilionidae", ref("papilionidae")));
        EntityRefLinker linker = linkerWith(Map.of("papilionidae", "/insects/families/papilionidae"));

        String url = CladeRankLinks.forStep(catalog, linker, "papilionidae", LinealRank.FAMILY);

        assertThat(url).isEqualTo("/insects/families/papilionidae");
    }

    @Test
    void resolvedOrderLinksToItsCatalogPage() {
        Catalog catalog = catalogWith(Map.of("lepidoptera", ref("lepidoptera")));
        EntityRefLinker linker = linkerWith(Map.of("lepidoptera", "/insects/orders/lepidoptera"));

        String url = CladeRankLinks.forStep(catalog, linker, "lepidoptera", LinealRank.ORDER);

        assertThat(url).isEqualTo("/insects/orders/lepidoptera");
    }

    @Test
    void unmappedFamilyFallsBackToRankConcept() {
        // Termitoidae is a ranked FAMILY clade with no insects family entity.
        String url = CladeRankLinks.forStep(
                catalogWith(Map.of()), linkerWith(Map.of()), "termitoidae", LinealRank.FAMILY);

        assertThat(url).isEqualTo("/concepts/family");
    }

    @Test
    void rankAboveTheCatalogFallsBackToConcept() {
        // Kingdom Animalia sits above the insects catalog entirely.
        String url = CladeRankLinks.forStep(
                catalogWith(Map.of()), linkerWith(Map.of()), "animalia", LinealRank.KINGDOM);

        assertThat(url).isEqualTo("/concepts/kingdom");
    }

    @Test
    void resolvedRefWithNoLinkerFallsBackToConcept() {
        // The catalog owns the slug, but no linker recognises its type.
        Catalog catalog = catalogWith(Map.of("papilionidae", ref("papilionidae")));
        EntityRefLinker linker = linkerWith(Map.of());

        String url = CladeRankLinks.forStep(catalog, linker, "papilionidae", LinealRank.FAMILY);

        assertThat(url).isEqualTo("/concepts/family");
    }
}
