package com.naturalist.catalog.inmem;

import com.naturalist.catalog.*;
import com.naturalist.catalog.CatalogContribution.SearchableEntity;
import com.naturalist.chemistry.TestChemistryIdentifiers.Compounds;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.TestInsectsIdentifiers.InsectSpecies;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.TestPlantsIdentifiers;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryCatalogTest {

    // Test-local DomainId records — the kernel test stays decoupled from the
    // production per-domain api modules (plants-api, chemistry-api,
    // insects-api). The slug values match production for readability of
    // assertions but the types are deliberately distinct.
    private record Plants() implements DomainId {
        @Override
        public String value() {
            return "plants";
        }
    }

    private record Chemistry() implements DomainId {
        @Override
        public String value() {
            return "chemistry";
        }
    }

    private record Insects() implements DomainId {
        @Override
        public String value() {
            return "insects";
        }
    }

    private static final EntityRef CALIFORNIA_PIPEVINE = new EntityRef(
            new Plants(), TestPlantsIdentifiers.Plants.CaliforniaPipevine.name);
    private static final EntityRef BORAGE = new EntityRef(
            new Plants(), TestPlantsIdentifiers.Plants.Borage.name);
    private static final EntityRef CRIMSON_CLOVER = new EntityRef(
            new Plants(), PlantName.of("crimson-clover"));
    private static final EntityRef WHITE_CLOVER = new EntityRef(
            new Plants(), PlantName.of("white-clover"));
    private static final EntityRef THYMOL = new EntityRef(
            new Chemistry(), Compounds.Thymol.name);
    private static final EntityRef TACHINID_FLY = new EntityRef(
            new Insects(), InsectSpecies.TachinidFly.name);

    private static final CompoundName THYMOL_NAME = Compounds.Thymol.name;
    private static final CompoundName UNKNOWN_COMPOUND = Compounds.NotFound.name;

    private static CatalogContribution contribution(DomainId domain, SearchableEntity... entities) {
        return new CatalogContribution() {
            @Override
            public DomainId domain() {
                return domain;
            }

            @Override
            public Stream<SearchableEntity> searchableEntities() {
                return Stream.of(entities);
            }
        };
    }

    private static SearchableEntity entity(EntityRef target, String... tokens) {
        return new SearchableEntity(target, Stream.of(tokens));
    }

    private static <T extends EntityName> EntityReferences<T> provider(
            DomainId domain, Class<T> referenceType, Map<T, List<EntityRef>> table) {
        return new EntityReferences<T>() {
            @Override
            public DomainId domain() {
                return domain;
            }

            @Override
            public Class<T> referenceType() {
                return referenceType;
            }

            @Override
            public Stream<EntityRef> referencesTo(T target) {
                return table.getOrDefault(target, List.of()).stream();
            }
        };
    }

    private static <T extends EntityName> EntityReferences<T> throwingProvider(
            DomainId domain, Class<T> referenceType) {
        return new EntityReferences<T>() {
            @Override
            public DomainId domain() {
                return domain;
            }

            @Override
            public Class<T> referenceType() {
                return referenceType;
            }

            @Override
            public Stream<EntityRef> referencesTo(T target) {
                throw new RuntimeException("simulated provider failure");
            }
        };
    }

    // -----------------------------------------------------------------
    // Search direction
    // -----------------------------------------------------------------

    @Test
    void exactSlugMatchYieldsExactSlugHit() {
        Catalog catalog = CatalogAssembly.from(contribution(new Plants(),
                entity(CALIFORNIA_PIPEVINE, "Aristolochia californica")));

        SearchResults results = catalog.search("california-pipevine");

        assertThat(results.stream()).hasSize(1);
        SearchHit hit = results.stream().findFirst().orElseThrow();
        assertThat(hit.target()).isEqualTo(CALIFORNIA_PIPEVINE);
        assertThat(hit.kind()).isEqualTo(MatchKind.EXACT_SLUG);
    }

    @Test
    void exactTokenMatchYieldsExactTokenHit() {
        Catalog catalog = CatalogAssembly.from(contribution(new Plants(),
                entity(CALIFORNIA_PIPEVINE, "Aristolochia californica", "Aristolochia")));

        SearchResults results = catalog.search("aristolochia");

        assertThat(results.stream())
                .extracting(SearchHit::kind)
                .containsExactly(MatchKind.EXACT_TOKEN);
        assertThat(results.stream().findFirst().orElseThrow().target())
                .isEqualTo(CALIFORNIA_PIPEVINE);
    }

    @Test
    void genusLevelAmbiguityReturnsAllMatches() {
        Catalog catalog = CatalogAssembly.from(contribution(new Plants(),
                entity(CRIMSON_CLOVER, "Trifolium pratense", "Trifolium"),
                entity(WHITE_CLOVER, "Trifolium repens", "Trifolium")));

        SearchResults results = catalog.search("trifolium");

        assertThat(results.stream())
                .extracting(SearchHit::target)
                .containsExactlyInAnyOrder(CRIMSON_CLOVER, WHITE_CLOVER);
        assertThat(results.stream())
                .extracting(SearchHit::kind)
                .containsOnly(MatchKind.EXACT_TOKEN);
    }

    @Test
    void searchIsCaseInsensitive() {
        Catalog catalog = CatalogAssembly.from(contribution(new Plants(),
                entity(CALIFORNIA_PIPEVINE, "Aristolochia californica")));

        SearchResults lower = catalog.search("aristolochia");
        SearchResults upper = catalog.search("ARISTOLOCHIA");
        SearchResults mixed = catalog.search("ArIstOlOchIa");

        assertThat(lower.stream()).hasSize(1);
        assertThat(upper.stream()).hasSize(1);
        assertThat(mixed.stream()).hasSize(1);
        assertThat(lower.stream().findFirst().orElseThrow().target())
                .isEqualTo(CALIFORNIA_PIPEVINE);
    }

    @Test
    void prefixMatchYieldsPrefixHit() {
        Catalog catalog = CatalogAssembly.from(contribution(new Plants(),
                entity(CALIFORNIA_PIPEVINE, "Aristolochia californica")));

        SearchResults results = catalog.search("arist");

        assertThat(results.stream())
                .extracting(SearchHit::kind)
                .containsOnly(MatchKind.PREFIX);
        assertThat(results.stream())
                .extracting(SearchHit::target)
                .containsExactly(CALIFORNIA_PIPEVINE);
    }

    @Test
    void exactMatchPreferredOverPrefix() {
        // Token "borage" exact-matches Borage's slug. Token "bor" would
        // prefix-match it — but we're not querying that. We're querying the
        // exact slug, so the kind must be EXACT_SLUG, not PREFIX, regardless
        // of any longer indexed tokens that also start with "borage".
        Catalog catalog = CatalogAssembly.from(contribution(new Plants(),
                entity(BORAGE, "borage", "borage officinalis")));

        SearchResults results = catalog.search("borage");

        SearchHit hit = results.stream().findFirst().orElseThrow();
        assertThat(hit.kind()).isEqualTo(MatchKind.EXACT_SLUG);
    }

    @Test
    void abbreviatedBinomialResolvesIndependentlyOfFullForm() {
        // "A. californica" tokenises to "a" + "californica"; both individually
        // index against the same target, so the search resolves the entity
        // through either token's exact-match path.
        Catalog catalog = CatalogAssembly.from(contribution(new Plants(),
                entity(CALIFORNIA_PIPEVINE, "A. californica")));

        SearchResults results = catalog.search("californica");

        assertThat(results.stream())
                .extracting(SearchHit::target)
                .containsExactly(CALIFORNIA_PIPEVINE);
    }

    @Test
    void hitsOrderedByKindThenSlug() {
        // Two entities with different slug starting letters; an exact-slug
        // match for one and an exact-token match for the other should sort
        // EXACT_SLUG ahead of EXACT_TOKEN.
        Catalog catalog = CatalogAssembly.from(contribution(new Plants(),
                entity(BORAGE, "borage", "shared"),
                entity(CALIFORNIA_PIPEVINE, "shared")));

        SearchResults results = catalog.search("shared borage");

        // borage has an EXACT_SLUG match for "borage"; california-pipevine has
        // only EXACT_TOKEN matches for "shared". borage must come first.
        assertThat(results.stream())
                .extracting(SearchHit::target)
                .containsExactly(BORAGE, CALIFORNIA_PIPEVINE);
    }

    @Test
    void emptyInputReturnsEmptyResults() {
        Catalog catalog = CatalogAssembly.from(contribution(new Plants(),
                entity(BORAGE, "borage")));

        assertThat(catalog.search("").stream()).isEmpty();
        assertThat(catalog.search("   ").stream()).isEmpty();
    }

    @Test
    void nullInputReturnsEmptyResults() {
        Catalog catalog = CatalogAssembly.from(contribution(new Plants(),
                entity(BORAGE, "borage")));

        assertThat(catalog.search(null).stream()).isEmpty();
    }

    @Test
    void unknownTokenReturnsEmptyResults() {
        Catalog catalog = CatalogAssembly.from(contribution(new Plants(),
                entity(BORAGE, "borage")));

        SearchResults results = catalog.search("zzz-no-such-thing");

        assertThat(results.stream()).isEmpty();
    }

    @Test
    void emptyContributionListYieldsEmptyCatalog() {
        Catalog catalog = CatalogAssembly.from(List.of());

        assertThat(catalog.search("anything").stream()).isEmpty();
    }

    @Test
    void contributionWithNoEntitiesIsLegal() {
        Catalog catalog = CatalogAssembly.from(contribution(new Plants()));

        assertThat(catalog.search("anything").stream()).isEmpty();
    }

    @Test
    void multipleContributionsCompose() {
        Catalog catalog = CatalogAssembly.from(
                contribution(new Plants(),
                        entity(CALIFORNIA_PIPEVINE, "Aristolochia californica")),
                contribution(new Chemistry(),
                        entity(THYMOL, "thymol")));

        assertThat(catalog.search("aristolochia").stream())
                .extracting(SearchHit::target)
                .containsExactly(CALIFORNIA_PIPEVINE);
        assertThat(catalog.search("thymol").stream())
                .extracting(SearchHit::target)
                .containsExactly(THYMOL);
    }

    @Test
    void searchableEntityInvariantsRejectNullTarget() {
        Observer observer = Observer.forClass(InMemoryCatalogTest.class);
        SearchableEntity entity = new SearchableEntity(null, Stream.empty());

        InvariantObservation result = observer.forMethod("searchableEntityInvariantsRejectNullTarget")
                .observable(entity, "entity");

        assertThat(result.violationNames())
                .anyMatch(n -> n.contains(".target"));
    }

    @Test
    void searchableEntityInvariantsRejectNullTokenStream() {
        Observer observer = Observer.forClass(InMemoryCatalogTest.class);
        SearchableEntity entity = new SearchableEntity(BORAGE, null);

        InvariantObservation result = observer.forMethod("searchableEntityInvariantsRejectNullTokenStream")
                .observable(entity, "entity");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".tokens"));
    }

    @Test
    void groupedByDomainPreservesWithinDomainOrdering() {
        Catalog catalog = CatalogAssembly.from(
                contribution(new Plants(),
                        entity(BORAGE, "borage"),
                        entity(CALIFORNIA_PIPEVINE, "borage")),
                contribution(new Chemistry(),
                        entity(THYMOL, "borage")));

        SearchResults results = catalog.search("borage");
        Map<DomainId, List<SearchHit>> grouped = results.groupedByDomain();

        assertThat(grouped.keySet()).contains(new Plants(), new Chemistry());
        assertThat(grouped.get(new Plants()))
                .extracting(SearchHit::target)
                .containsExactly(BORAGE, CALIFORNIA_PIPEVINE);
    }

    // -----------------------------------------------------------------
    // Inverse direction (unchanged from M3)
    // -----------------------------------------------------------------

    @Test
    void domainsReferencingReturnsDomainsWithMatchingProviders() {
        EntityReferences<CompoundName> plantsRefs = provider(new Plants(),
                CompoundName.class, Map.of());
        EntityReferences<CompoundName> insectsRefs = provider(new Insects(),
                CompoundName.class, Map.of());
        Catalog catalog = CatalogAssembly.from(List.of(), List.of(plantsRefs, insectsRefs));

        assertThat(catalog.domainsReferencing(CompoundName.class))
                .containsExactlyInAnyOrder(new Plants(), new Insects());
    }

    @Test
    void domainsReferencingReturnsEmptyForUnknownType() {
        EntityReferences<CompoundName> plantsRefs = provider(new Plants(),
                CompoundName.class, Map.of());
        Catalog catalog = CatalogAssembly.from(List.of(), List.of(plantsRefs));

        assertThat(catalog.domainsReferencing(InsectSpeciesName.class)).isEmpty();
    }

    @Test
    void domainsReferencingReturnsEmptyForNullType() {
        Catalog catalog = CatalogAssembly.from(List.of(), List.of());

        assertThat(catalog.domainsReferencing(null)).isEmpty();
    }

    @Test
    void findReferencesToGroupsByDomain() {
        EntityReferences<CompoundName> plantsRefs = provider(new Plants(),
                CompoundName.class,
                Map.of(THYMOL_NAME, List.of(CALIFORNIA_PIPEVINE)));
        EntityReferences<CompoundName> insectsRefs = provider(new Insects(),
                CompoundName.class,
                Map.of(THYMOL_NAME, List.of(TACHINID_FLY)));
        Catalog catalog = CatalogAssembly.from(List.of(), List.of(plantsRefs, insectsRefs));

        Map<DomainId, List<EntityRef>> result = catalog.findReferencesTo(THYMOL_NAME);

        assertThat(result).containsOnlyKeys(new Plants(), new Insects());
        assertThat(result.get(new Plants())).containsExactly(CALIFORNIA_PIPEVINE);
        assertThat(result.get(new Insects())).containsExactly(TACHINID_FLY);
    }

    @Test
    void findReferencesToReturnsEmptyMapForUnknownTarget() {
        EntityReferences<CompoundName> plantsRefs = provider(new Plants(),
                CompoundName.class,
                Map.of(THYMOL_NAME, List.of(CALIFORNIA_PIPEVINE)));
        Catalog catalog = CatalogAssembly.from(List.of(), List.of(plantsRefs));

        Map<DomainId, List<EntityRef>> result = catalog.findReferencesTo(UNKNOWN_COMPOUND);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void findReferencesToReturnsEmptyMapForNullTarget() {
        Catalog catalog = CatalogAssembly.from(List.of(), List.of());

        assertThat(catalog.findReferencesTo(null)).isNotNull().isEmpty();
    }

    @Test
    void findReferencesToSurvivesAThrowingProvider() {
        EntityReferences<CompoundName> plantsRefs = throwingProvider(
                new Plants(), CompoundName.class);
        EntityReferences<CompoundName> insectsRefs = provider(new Insects(),
                CompoundName.class,
                Map.of(THYMOL_NAME, List.of(TACHINID_FLY)));
        Catalog catalog = CatalogAssembly.from(List.of(), List.of(plantsRefs, insectsRefs));

        Map<DomainId, List<EntityRef>> result = catalog.findReferencesTo(THYMOL_NAME);

        assertThat(result).containsOnlyKeys(new Insects());
        assertThat(result.get(new Insects())).containsExactly(TACHINID_FLY);
    }

    @Test
    void domainsReferencingResultIsImmutable() {
        EntityReferences<CompoundName> plantsRefs = provider(new Plants(),
                CompoundName.class, Map.of());
        Catalog catalog = CatalogAssembly.from(List.of(), List.of(plantsRefs));

        Set<DomainId> result = catalog.domainsReferencing(CompoundName.class);

        assertThatThrownBy(() -> result.add(new Insects()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findReferencesToResultIsImmutable() {
        EntityReferences<CompoundName> plantsRefs = provider(new Plants(),
                CompoundName.class,
                Map.of(THYMOL_NAME, List.of(CALIFORNIA_PIPEVINE)));
        Catalog catalog = CatalogAssembly.from(List.of(), List.of(plantsRefs));

        Map<DomainId, List<EntityRef>> result = catalog.findReferencesTo(THYMOL_NAME);

        assertThatThrownBy(() -> result.put(new Insects(), List.of()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.get(new Plants()).add(BORAGE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // -----------------------------------------------------------------
    // Open-DomainId slug uniqueness (M3)
    // -----------------------------------------------------------------

    @Test
    void duplicateSlugAcrossDistinctDomainTypesFailsAtAssembly() {
        record RoguePlants() implements DomainId {
            @Override
            public String value() {
                return "plants";
            }
        }
        var legitimate = contribution(new Plants(), entity(BORAGE, "borage"));
        var rogue = contribution(new RoguePlants(), entity(CALIFORNIA_PIPEVINE, "shared"));

        assertThatThrownBy(() -> CatalogAssembly.from(List.of(legitimate, rogue), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plants")
                .hasMessageContaining("RoguePlants");
    }

    @Test
    void duplicateSlugBetweenContributionAndProviderFailsAtAssembly() {
        record RoguePlants() implements DomainId {
            @Override
            public String value() {
                return "plants";
            }
        }
        var contribution = contribution(new Plants(), entity(BORAGE, "borage"));
        EntityReferences<CompoundName> rogueProvider = provider(new RoguePlants(),
                CompoundName.class, Map.of());

        assertThatThrownBy(() -> CatalogAssembly.from(List.of(contribution), List.of(rogueProvider)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plants");
    }

    @Test
    void sameDomainAcrossMultipleContributionsAndProvidersIsLegal() {
        var first = contribution(new Plants(), entity(BORAGE, "borage"));
        var second = contribution(new Plants(), entity(CALIFORNIA_PIPEVINE, "pipevine"));
        EntityReferences<CompoundName> refs = provider(new Plants(),
                CompoundName.class, Map.of());

        Catalog catalog = CatalogAssembly.from(List.of(first, second), List.of(refs));

        assertThat(catalog.search("borage").stream())
                .extracting(SearchHit::target)
                .containsExactly(BORAGE);
    }

    @Test
    void searchOnlyCatalogStillExposesEmptyInverseSurface() {
        Catalog catalog = CatalogAssembly.from(contribution(new Plants(),
                entity(BORAGE, "borage")));

        assertThat(catalog.domainsReferencing(CompoundName.class)).isEmpty();
        assertThat(catalog.findReferencesTo(THYMOL_NAME)).isEmpty();
    }
}
