package com.naturalist.atlas;

import com.naturalist.atlas.AtlasContribution.Alias;
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
import com.naturalist.plants.TestPlantsIdentifiers;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultAtlasTest {

    private static final EntityRef CALIFORNIA_PIPEVINE = new EntityRef(
            new Plants(), TestPlantsIdentifiers.Plants.CaliforniaPipevine.name);
    private static final EntityRef BORAGE = new EntityRef(
            new Plants(), TestPlantsIdentifiers.Plants.Borage.name);
    private static final EntityRef THYMOL = new EntityRef(
            new Chemistry(), Compounds.Thymol.name);
    private static final EntityRef TACHINID_FLY = new EntityRef(
            new Insects(), InsectSpecies.TachinidFly.name);

    private static final CompoundName THYMOL_NAME = Compounds.Thymol.name;
    private static final CompoundName UNKNOWN_COMPOUND = Compounds.NotFound.name;

    private static AtlasContribution contribution(DomainId domain, Alias... aliases) {
        return new AtlasContribution() {
            @Override public DomainId domain() { return domain; }
            @Override public Stream<Alias> aliases() { return Stream.of(aliases); }
        };
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

    @Test
    void exactSurfaceFormResolves() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("Aristolochia californica", CALIFORNIA_PIPEVINE)));

        assertThat(atlas.resolveAlias("Aristolochia californica"))
                .contains(CALIFORNIA_PIPEVINE);
    }

    @Test
    void slugFormResolves() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("california-pipevine", CALIFORNIA_PIPEVINE)));

        assertThat(atlas.resolveAlias("california-pipevine"))
                .contains(CALIFORNIA_PIPEVINE);
    }

    @Test
    void caseMismatchedSurfaceFormDoesNotResolve() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("Aristolochia californica", CALIFORNIA_PIPEVINE)));

        // Genus capitalisation matters: lower-case "aristolochia" is not the
        // genus and must not collide with "Aristolochia".
        assertThat(atlas.resolveAlias("aristolochia californica")).isEmpty();
        assertThat(atlas.resolveAlias("ARISTOLOCHIA CALIFORNICA")).isEmpty();
    }

    @Test
    void unknownSurfaceFormReturnsEmpty() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("borage", BORAGE)));

        assertThat(atlas.resolveAlias("battus-philenor")).isEmpty();
    }

    @Test
    void nullTextReturnsEmpty() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("borage", BORAGE)));

        assertThat(atlas.resolveAlias(null)).isEmpty();
    }

    @Test
    void emptyTextReturnsEmpty() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("borage", BORAGE)));

        assertThat(atlas.resolveAlias("")).isEmpty();
    }

    @Test
    void emptyContributionListYieldsEmptyAtlas() {
        Atlas atlas = AtlasAssembly.from(List.of());

        assertThat(atlas.resolveAlias("anything")).isEmpty();
    }

    @Test
    void contributionWithNoAliasesIsLegal() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants()));

        assertThat(atlas.resolveAlias("anything")).isEmpty();
    }

    @Test
    void multipleContributionsCompose() {
        Atlas atlas = AtlasAssembly.from(
                contribution(new Plants(),
                        new Alias("Aristolochia californica", CALIFORNIA_PIPEVINE)),
                contribution(new Chemistry(),
                        new Alias("thymol", THYMOL)));

        assertThat(atlas.resolveAlias("Aristolochia californica"))
                .contains(CALIFORNIA_PIPEVINE);
        assertThat(atlas.resolveAlias("thymol"))
                .contains(THYMOL);
    }

    @Test
    void differentLengthSurfaceFormsCoexist() {
        // "Aristolochia" (genus) and "Aristolochia californica" (species) are
        // different keys; both store independently. The renderer is the layer
        // responsible for preferring the longer match in prose; the atlas
        // simply answers each lookup honestly.
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("Aristolochia", CALIFORNIA_PIPEVINE),
                new Alias("Aristolochia californica", CALIFORNIA_PIPEVINE)));

        assertThat(atlas.resolveAlias("Aristolochia")).contains(CALIFORNIA_PIPEVINE);
        assertThat(atlas.resolveAlias("Aristolochia californica")).contains(CALIFORNIA_PIPEVINE);
    }

    @Test
    void identicalSurfaceFormToSameTargetIsDeduplicated() {
        // Two contributions registering the same surface form to the same
        // target is benign — assembly should not fail.
        Atlas atlas = AtlasAssembly.from(
                contribution(new Plants(),
                        new Alias("california-pipevine", CALIFORNIA_PIPEVINE)),
                contribution(new Plants(),
                        new Alias("california-pipevine", CALIFORNIA_PIPEVINE)));

        assertThat(atlas.resolveAlias("california-pipevine"))
                .contains(CALIFORNIA_PIPEVINE);
    }

    @Test
    void identicalSurfaceFormToDifferentTargetsIsAssemblyError() {
        // Two contributions both claiming "Aristolochia" — one points at the
        // pipevine plant, another at thymol. Equal-length match → fail.
        AtlasContribution plants = contribution(new Plants(),
                new Alias("Aristolochia", CALIFORNIA_PIPEVINE));
        AtlasContribution chemistry = contribution(new Chemistry(),
                new Alias("Aristolochia", THYMOL));

        assertThatThrownBy(() -> AtlasAssembly.from(plants, chemistry))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aristolochia");
    }

    @Test
    void assemblyErrorMentionsBothTargets() {
        AtlasContribution plants = contribution(new Plants(),
                new Alias("Aristolochia", CALIFORNIA_PIPEVINE));
        AtlasContribution chemistry = contribution(new Chemistry(),
                new Alias("Aristolochia", THYMOL));

        assertThatThrownBy(() -> AtlasAssembly.from(plants, chemistry))
                .hasMessageContaining(TestPlantsIdentifiers.Plants.CaliforniaPipevine.name.value())
                .hasMessageContaining(Compounds.Thymol.name.value());
    }

    @Test
    void aliasInvariantsRejectBlankSurfaceForm() {
        Observer observer = Observer.forClass(DefaultAtlasTest.class);
        Alias alias = new Alias("   ", CALIFORNIA_PIPEVINE);

        InvariantObservation result = observer.forMethod("aliasInvariantsRejectBlankSurfaceForm")
                .observable(alias, "alias");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".surfaceForm"));
    }

    @Test
    void aliasInvariantsRejectNullTarget() {
        Observer observer = Observer.forClass(DefaultAtlasTest.class);
        Alias alias = new Alias("Aristolochia", null);

        InvariantObservation result = observer.forMethod("aliasInvariantsRejectNullTarget")
                .observable(alias, "alias");

        assertThat(result.violationNames())
                .anyMatch(n -> n.contains(".target"));
    }

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

        // No provider declared for InsectSpeciesName — routing answers empty.
        assertThat(atlas.domainsReferencing(InsectSpeciesName.class)).isEmpty();
    }

    @Test
    void domainsReferencingReturnsEmptyForNullType() {
        Atlas atlas = AtlasAssembly.from(List.of(), List.of());

        assertThat(atlas.domainsReferencing(null)).isEmpty();
    }

    @Test
    void domainsReferencingDeduplicatesProvidersFromSameDomain() {
        // A domain may register two providers for the same type when its
        // sub-contexts each own a slice — routing answers with one entry.
        EntityReferences<CompoundName> first = provider(new Plants(),
                CompoundName.class, Map.of());
        EntityReferences<CompoundName> second = provider(new Plants(),
                CompoundName.class, Map.of());
        Atlas atlas = AtlasAssembly.from(List.of(), List.of(first, second));

        assertThat(atlas.domainsReferencing(CompoundName.class))
                .containsExactly(new Plants());
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
    void findReferencesToConcatenatesProvidersWithinSameDomain() {
        EntityReferences<CompoundName> first = provider(new Plants(),
                CompoundName.class,
                Map.of(THYMOL_NAME, List.of(CALIFORNIA_PIPEVINE)));
        EntityReferences<CompoundName> second = provider(new Plants(),
                CompoundName.class,
                Map.of(THYMOL_NAME, List.of(BORAGE)));
        Atlas atlas = AtlasAssembly.from(List.of(), List.of(first, second));

        Map<DomainId, List<EntityRef>> result = atlas.findReferencesTo(THYMOL_NAME);

        assertThat(result.get(new Plants()))
                .containsExactly(CALIFORNIA_PIPEVINE, BORAGE);
    }

    @Test
    void findReferencesToReturnsEmptyMapForUnknownTarget() {
        EntityReferences<CompoundName> plantsRefs = provider(new Plants(),
                CompoundName.class,
                Map.of(THYMOL_NAME, List.of(CALIFORNIA_PIPEVINE)));
        Atlas atlas = AtlasAssembly.from(List.of(), List.of(plantsRefs));

        // Provider exists for the type but has nothing for this slug — empty
        // map (not null), and no domain key with an empty list.
        Map<DomainId, List<EntityRef>> result = atlas.findReferencesTo(UNKNOWN_COMPOUND);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void findReferencesToReturnsEmptyMapWhenNoProviderHandlesType() {
        EntityReferences<CompoundName> plantsRefs = provider(new Plants(),
                CompoundName.class, Map.of());
        Atlas atlas = AtlasAssembly.from(List.of(), List.of(plantsRefs));

        // No provider for InsectSpeciesName at all.
        Map<DomainId, List<EntityRef>> result = atlas.findReferencesTo(
                InsectSpecies.TachinidFly.name);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void findReferencesToReturnsEmptyMapForNullTarget() {
        Atlas atlas = AtlasAssembly.from(List.of(), List.of());

        assertThat(atlas.findReferencesTo(null)).isNotNull().isEmpty();
    }

    @Test
    void findReferencesToSurvivesAThrowingProvider() {
        // Plants throws; insects answers. Per the plan's exception policy,
        // peer provider results are returned regardless. (M9 wires the
        // observation; this milestone only requires graceful degradation.)
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
    void forwardOnlyAtlasStillExposesEmptyInverseSurface() {
        // Apps that wire only the M2 surface get empty answers, not nulls,
        // from the inverse methods — pages can render unconditionally.
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("california-pipevine", CALIFORNIA_PIPEVINE)));

        assertThat(atlas.domainsReferencing(CompoundName.class)).isEmpty();
        assertThat(atlas.findReferencesTo(THYMOL_NAME)).isEmpty();
    }

    @Test
    void resolutionIsHotPathSafeAcrossManyLookups() {
        // Smoke test against the documented contract: resolution is a hash
        // lookup, not iteration over contributions. With 1000 aliases a tight
        // loop should complete in milliseconds.
        Alias[] aliases = new Alias[1000];
        for (int i = 0; i < aliases.length; i++) {
            aliases[i] = new Alias("alias-" + i, BORAGE);
        }
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(), aliases));

        Optional<EntityRef> last = Optional.empty();
        for (int i = 0; i < aliases.length; i++) {
            last = atlas.resolveAlias("alias-" + i);
        }

        assertThat(last).contains(BORAGE);
    }
}
