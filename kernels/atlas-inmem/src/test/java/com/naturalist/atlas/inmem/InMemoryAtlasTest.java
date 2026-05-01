package com.naturalist.atlas.inmem;

import com.naturalist.atlas.*;
import com.naturalist.atlas.AtlasContribution.SearchableEntity;
import com.naturalist.atlas.DomainId.Chemistry;
import com.naturalist.atlas.DomainId.Insects;
import com.naturalist.atlas.DomainId.Plants;
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

class InMemoryAtlasTest {

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

    private static AtlasContribution contribution(DomainId domain, SearchableEntity... entities) {
        return new AtlasContribution() {
            @Override public DomainId domain() { return domain; }
            @Override public Stream<SearchableEntity> searchableEntities() { return Stream.of(entities); }
        };
    }

    private static SearchableEntity entity(EntityRef target, String... tokens) {
        return new SearchableEntity(target, Stream.of(tokens));
    }

    private static <T extends EntityName> EntityReferences<T> provider(
            DomainId domain, Class<T> referenceType, Map<T, List<EntityRef>> table) {
        return new EntityReferences<T>() {
            @Override public DomainId domain() { return domain; }
            @Override public Class<T> referenceType() { return referenceType; }
            @Override public Stream<EntityRef> referencesTo(T target) {
                return table.getOrDefault(target, List.of()).stream();
            }
        };
    }

    private static <T extends EntityName> EntityReferences<T> throwingProvider(
            DomainId domain, Class<T> referenceType) {
        return new EntityReferences<T>() {
            @Override public DomainId domain() { return domain; }
            @Override public Class<T> referenceType() { return referenceType; }
            @Override public Stream<EntityRef> referencesTo(T target) {
                throw new RuntimeException("simulated provider failure");
            }
        };
    }

    // -----------------------------------------------------------------
    // Search direction
    // -----------------------------------------------------------------

    @Test
    void exactSlugMatchYieldsExactSlugHit() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                entity(CALIFORNIA_PIPEVINE, "Aristolochia californica")));

        SearchResults results = atlas.search("california-pipevine");

        assertThat(results.stream()).hasSize(1);
        SearchHit hit = results.stream().findFirst().orElseThrow();
        assertThat(hit.target()).isEqualTo(CALIFORNIA_PIPEVINE);
        assertThat(hit.kind()).isEqualTo(MatchKind.EXACT_SLUG);
    }

    @Test
    void exactTokenMatchYieldsExactTokenHit() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                entity(CALIFORNIA_PIPEVINE, "Aristolochia californica", "Aristolochia")));

        SearchResults results = atlas.search("aristolochia");

        assertThat(results.stream())
                .extracting(SearchHit::kind)
                .containsExactly(MatchKind.EXACT_TOKEN);
        assertThat(results.stream().findFirst().orElseThrow().target())
                .isEqualTo(CALIFORNIA_PIPEVINE);
    }

    @Test
    void genusLevelAmbiguityReturnsAllMatches() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                entity(CRIMSON_CLOVER, "Trifolium pratense", "Trifolium"),
                entity(WHITE_CLOVER, "Trifolium repens", "Trifolium")));

        SearchResults results = atlas.search("trifolium");

        assertThat(results.stream())
                .extracting(SearchHit::target)
                .containsExactlyInAnyOrder(CRIMSON_CLOVER, WHITE_CLOVER);
        assertThat(results.stream())
                .extracting(SearchHit::kind)
                .containsOnly(MatchKind.EXACT_TOKEN);
    }

    @Test
    void searchIsCaseInsensitive() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                entity(CALIFORNIA_PIPEVINE, "Aristolochia californica")));

        SearchResults lower = atlas.search("aristolochia");
        SearchResults upper = atlas.search("ARISTOLOCHIA");
        SearchResults mixed = atlas.search("ArIstOlOchIa");

        assertThat(lower.stream()).hasSize(1);
        assertThat(upper.stream()).hasSize(1);
        assertThat(mixed.stream()).hasSize(1);
        assertThat(lower.stream().findFirst().orElseThrow().target())
                .isEqualTo(CALIFORNIA_PIPEVINE);
    }

    @Test
    void prefixMatchYieldsPrefixHit() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                entity(CALIFORNIA_PIPEVINE, "Aristolochia californica")));

        SearchResults results = atlas.search("arist");

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
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                entity(BORAGE, "borage", "borage officinalis")));

        SearchResults results = atlas.search("borage");

        SearchHit hit = results.stream().findFirst().orElseThrow();
        assertThat(hit.kind()).isEqualTo(MatchKind.EXACT_SLUG);
    }

    @Test
    void abbreviatedBinomialResolvesIndependentlyOfFullForm() {
        // "A. californica" tokenises to "a" + "californica"; both individually
        // index against the same target, so the search resolves the entity
        // through either token's exact-match path.
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                entity(CALIFORNIA_PIPEVINE, "A. californica")));

        SearchResults results = atlas.search("californica");

        assertThat(results.stream())
                .extracting(SearchHit::target)
                .containsExactly(CALIFORNIA_PIPEVINE);
    }

    @Test
    void hitsOrderedByKindThenSlug() {
        // Two entities with different slug starting letters; an exact-slug
        // match for one and an exact-token match for the other should sort
        // EXACT_SLUG ahead of EXACT_TOKEN.
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                entity(BORAGE, "borage", "shared"),
                entity(CALIFORNIA_PIPEVINE, "shared")));

        SearchResults results = atlas.search("shared borage");

        // borage has an EXACT_SLUG match for "borage"; california-pipevine has
        // only EXACT_TOKEN matches for "shared". borage must come first.
        assertThat(results.stream())
                .extracting(SearchHit::target)
                .containsExactly(BORAGE, CALIFORNIA_PIPEVINE);
    }

    @Test
    void emptyInputReturnsEmptyResults() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                entity(BORAGE, "borage")));

        assertThat(atlas.search("").stream()).isEmpty();
        assertThat(atlas.search("   ").stream()).isEmpty();
    }

    @Test
    void nullInputReturnsEmptyResults() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                entity(BORAGE, "borage")));

        assertThat(atlas.search(null).stream()).isEmpty();
    }

    @Test
    void unknownTokenReturnsEmptyResults() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                entity(BORAGE, "borage")));

        SearchResults results = atlas.search("zzz-no-such-thing");

        assertThat(results.stream()).isEmpty();
    }

    @Test
    void emptyContributionListYieldsEmptyAtlas() {
        Atlas atlas = AtlasAssembly.from(List.of());

        assertThat(atlas.search("anything").stream()).isEmpty();
    }

    @Test
    void contributionWithNoEntitiesIsLegal() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants()));

        assertThat(atlas.search("anything").stream()).isEmpty();
    }

    @Test
    void multipleContributionsCompose() {
        Atlas atlas = AtlasAssembly.from(
                contribution(new Plants(),
                        entity(CALIFORNIA_PIPEVINE, "Aristolochia californica")),
                contribution(new Chemistry(),
                        entity(THYMOL, "thymol")));

        assertThat(atlas.search("aristolochia").stream())
                .extracting(SearchHit::target)
                .containsExactly(CALIFORNIA_PIPEVINE);
        assertThat(atlas.search("thymol").stream())
                .extracting(SearchHit::target)
                .containsExactly(THYMOL);
    }

    @Test
    void searchableEntityInvariantsRejectNullTarget() {
        Observer observer = Observer.forClass(InMemoryAtlasTest.class);
        SearchableEntity entity = new SearchableEntity(null, Stream.empty());

        InvariantObservation result = observer.forMethod("searchableEntityInvariantsRejectNullTarget")
                .observable(entity, "entity");

        assertThat(result.violationNames())
                .anyMatch(n -> n.contains(".target"));
    }

    @Test
    void searchableEntityInvariantsRejectNullTokenStream() {
        Observer observer = Observer.forClass(InMemoryAtlasTest.class);
        SearchableEntity entity = new SearchableEntity(BORAGE, null);

        InvariantObservation result = observer.forMethod("searchableEntityInvariantsRejectNullTokenStream")
                .observable(entity, "entity");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".tokens"));
    }

    @Test
    void groupedByDomainPreservesWithinDomainOrdering() {
        Atlas atlas = AtlasAssembly.from(
                contribution(new Plants(),
                        entity(BORAGE, "borage"),
                        entity(CALIFORNIA_PIPEVINE, "borage")),
                contribution(new Chemistry(),
                        entity(THYMOL, "borage")));

        SearchResults results = atlas.search("borage");
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
        Atlas atlas = AtlasAssembly.from(List.of(), List.of(plantsRefs, insectsRefs));

        assertThat(atlas.domainsReferencing(CompoundName.class))
                .containsExactlyInAnyOrder(new Plants(), new Insects());
    }

    @Test
    void domainsReferencingReturnsEmptyForUnknownType() {
        EntityReferences<CompoundName> plantsRefs = provider(new Plants(),
                CompoundName.class, Map.of());
        Atlas atlas = AtlasAssembly.from(List.of(), List.of(plantsRefs));

        assertThat(atlas.domainsReferencing(InsectSpeciesName.class)).isEmpty();
    }

    @Test
    void domainsReferencingReturnsEmptyForNullType() {
        Atlas atlas = AtlasAssembly.from(List.of(), List.of());

        assertThat(atlas.domainsReferencing(null)).isEmpty();
    }

    @Test
    void findReferencesToGroupsByDomain() {
        EntityReferences<CompoundName> plantsRefs = provider(new Plants(),
                CompoundName.class,
                Map.of(THYMOL_NAME, List.of(CALIFORNIA_PIPEVINE)));
        EntityReferences<CompoundName> insectsRefs = provider(new Insects(),
                CompoundName.class,
                Map.of(THYMOL_NAME, List.of(TACHINID_FLY)));
        Atlas atlas = AtlasAssembly.from(List.of(), List.of(plantsRefs, insectsRefs));

        Map<DomainId, List<EntityRef>> result = atlas.findReferencesTo(THYMOL_NAME);

        assertThat(result).containsOnlyKeys(new Plants(), new Insects());
        assertThat(result.get(new Plants())).containsExactly(CALIFORNIA_PIPEVINE);
        assertThat(result.get(new Insects())).containsExactly(TACHINID_FLY);
    }

    @Test
    void findReferencesToReturnsEmptyMapForUnknownTarget() {
        EntityReferences<CompoundName> plantsRefs = provider(new Plants(),
                CompoundName.class,
                Map.of(THYMOL_NAME, List.of(CALIFORNIA_PIPEVINE)));
        Atlas atlas = AtlasAssembly.from(List.of(), List.of(plantsRefs));

        Map<DomainId, List<EntityRef>> result = atlas.findReferencesTo(UNKNOWN_COMPOUND);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void findReferencesToReturnsEmptyMapForNullTarget() {
        Atlas atlas = AtlasAssembly.from(List.of(), List.of());

        assertThat(atlas.findReferencesTo(null)).isNotNull().isEmpty();
    }

    @Test
    void findReferencesToSurvivesAThrowingProvider() {
        EntityReferences<CompoundName> plantsRefs = throwingProvider(
                new Plants(), CompoundName.class);
        EntityReferences<CompoundName> insectsRefs = provider(new Insects(),
                CompoundName.class,
                Map.of(THYMOL_NAME, List.of(TACHINID_FLY)));
        Atlas atlas = AtlasAssembly.from(List.of(), List.of(plantsRefs, insectsRefs));

        Map<DomainId, List<EntityRef>> result = atlas.findReferencesTo(THYMOL_NAME);

        assertThat(result).containsOnlyKeys(new Insects());
        assertThat(result.get(new Insects())).containsExactly(TACHINID_FLY);
    }

    @Test
    void domainsReferencingResultIsImmutable() {
        EntityReferences<CompoundName> plantsRefs = provider(new Plants(),
                CompoundName.class, Map.of());
        Atlas atlas = AtlasAssembly.from(List.of(), List.of(plantsRefs));

        Set<DomainId> result = atlas.domainsReferencing(CompoundName.class);

        assertThatThrownBy(() -> result.add(new Insects()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findReferencesToResultIsImmutable() {
        EntityReferences<CompoundName> plantsRefs = provider(new Plants(),
                CompoundName.class,
                Map.of(THYMOL_NAME, List.of(CALIFORNIA_PIPEVINE)));
        Atlas atlas = AtlasAssembly.from(List.of(), List.of(plantsRefs));

        Map<DomainId, List<EntityRef>> result = atlas.findReferencesTo(THYMOL_NAME);

        assertThatThrownBy(() -> result.put(new Insects(), List.of()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.get(new Plants()).add(BORAGE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void searchOnlyAtlasStillExposesEmptyInverseSurface() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                entity(BORAGE, "borage")));

        assertThat(atlas.domainsReferencing(CompoundName.class)).isEmpty();
        assertThat(atlas.findReferencesTo(THYMOL_NAME)).isEmpty();
    }
}
