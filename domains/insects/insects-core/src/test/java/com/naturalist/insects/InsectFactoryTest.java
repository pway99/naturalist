package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.lifestage.AdultStage;
import com.naturalist.insects.lifestage.EggStage;
import com.naturalist.insects.lifestage.LarvaStage;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.insects.lifestage.InsectLifeStageTestEntitySource;
import com.naturalist.insects.lifestage.LifeStage;
import com.naturalist.observability.Observer;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectFactoryTest {

    static final Observer observer = Observer.forClass(InsectFactoryTest.class);

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Wired InsectQuery.SpeciesQuery speciesQuery;
    @Wired InsectQuery.ImageQuery imageQuery;
    @Wired InsectQuery.GenusQuery genusQuery;
    @Wired InsectQuery.FamilyQuery familyQuery;
    @Wired InsectQuery.OrderQuery orderQuery;
    @Wired InsectCitationQueryImpl citationQuery;
    @Wired InsectFeatureQueryImpl featureQuery;
    @Wired InsectQuery.FunctionalRoleQuery roleQuery;

    /**
     * Stub lifestage query — returns empty collections. InsectLifeStageQueryImpl is
     * package-private inside {@code com.naturalist.insects.lifestage} and not accessible
     * here; insects-test-context depends on insects-core and cannot be reversed. The
     * factory's invariant check for non-null lifeStages is satisfied by the empty
     * collection, and the test assertions target the rank chain and citations.
     */
    InsectLifeStageQuery lifeStageQuery = new InsectLifeStageQuery() {
        @Override
        public LifeStageEntityQuery lifeStages() {
            return new LifeStageEntityQuery() {
                @Override
                public LifeStageCollection forParentName(InsectRankName parentName) {
                    return LifeStageCollection.empty();
                }

                @Override
                public Optional<LifeStage> getByName(com.naturalist.insects.LifeStageName name) {
                    return Optional.empty();
                }

                @Override
                public LifeStageCollection findByNameSet(Set<com.naturalist.insects.LifeStageName> names) {
                    return LifeStageCollection.empty();
                }

                @Override
                public Page<LifeStage> findPage(PageRequest pageRequest) {
                    return new Page<>(java.util.List.of(), pageRequest.pageNumber(), pageRequest.pageSize(), 0, false);
                }
            };
        }
    };

    InsectFactory factory;

    /** The factory under test, over the graph's queries and the stub life-stage query above. */
    @BeforeEach
    void buildFactory() {
        factory = new InsectFactory(
                speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery,
                lifeStageQuery, citationQuery, featureQuery, roleQuery);
    }

    @Test
    void buildByName_rejectsNull() {
        assertThatThrownBy(() -> factory.buildByName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("name");
    }

    @Test
    void buildByName_speciesName_returnsFullyPopulatedInsect() {
        InsectSpeciesName name = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;

        Optional<Insect> result = factory.buildByName(name);

        assertThat(result).isPresent();
        Insect insect = result.get();
        assertThat(insect.species()).isNotNull();
        assertThat(insect.genus()).isNotNull();
        assertThat(insect.family()).isNotNull();
        assertThat(insect.order()).isNotNull();
        assertThat(insect.observations()).isNotNull();
        assertThat(insect.lifeStages()).isNotNull();
        // Verify invariants pass for the fully assembled read model
        assertThat(observer.observable(insect, "insect").violations()).isEmpty();
    }

    @Test
    void buildByName_orderName_returnsOrderOnlyInsect() {
        InsectOrderName name = InsectOrderName.of("lepidoptera");

        Optional<Insect> result = factory.buildByName(name);

        assertThat(result).isPresent();
        Insect insect = result.get();
        assertThat(insect.order()).isNotNull();
        assertThat(insect.family()).isNull();
        assertThat(insect.genus()).isNull();
        assertThat(insect.species()).isNull();
        assertThat(insect.lifeStages()).isNotNull();
    }

    @Test
    void buildByName_familyName_returnsFamilyAndOrderInsect() {
        InsectFamilyName name = TestInsectsIdentifiers.InsectFamily.Papilionidae.name;

        Optional<Insect> result = factory.buildByName(name);

        assertThat(result).isPresent();
        Insect insect = result.get();
        assertThat(insect.family()).isNotNull();
        assertThat(insect.order()).isNotNull();
        assertThat(insect.genus()).isNull();
        assertThat(insect.species()).isNull();
        assertThat(insect.lifeStages()).isNotNull();
    }

    @Test
    void buildByName_unknownName_returnsEmpty() {
        Optional<Insect> result = factory.buildByName(
                TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(result).isEmpty();
    }

    @Test
    void buildByName_subspeciesName_returnsEmpty() {
        InsectSubspeciesName subspeciesName = InsectSubspeciesName.of("battus-philenor-philenor");

        Optional<Insect> result = factory.buildByName(subspeciesName);

        assertThat(result).isEmpty();
    }

    @Test
    void buildByName_speciesWithCitations_includesResolvedCitations() {
        InsectSpeciesName name = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;

        Optional<Insect> result = factory.buildByName(name);

        assertThat(result).isPresent();
        Insect insect = result.get();
        // battus-philenor inherits citations from papilionidae and lepidoptera
        assertThat(insect.citations()).isNotNull();
        assertThat(insect.citations().citations()).isNotEmpty();
        assertThat(insect.citations().citations())
                .extracting(c -> c.citation().name().value())
            .contains("eol-battus-philenor-130502", "eol-lepidoptera-747");
    }

    @Test
    void buildByName_composesFeatureView() {
        InsectSpeciesName name = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        Insect insect = factory.buildByName(name).orElseThrow();
        assertThat(insect.features()).isNotNull();
        assertThat(insect.features().subject()).isEqualTo(name);
    }

    @Test
    void buildByName_orderName_composesFamilyChildren() {
        Insect insect = factory.buildByName(InsectOrderName.of("lepidoptera")).orElseThrow();
        assertThat(insect.children()).isNotEmpty();
        assertThat(insect.children()).allSatisfy(c ->
                assertThat(c).isInstanceOf(InsectFamily.class));
    }

    @Test
    void buildByName_orderName_familyChildrenAreAlphabeticallySorted() {
        Insect insect = factory.buildByName(InsectOrderName.of("lepidoptera")).orElseThrow();

        assertThat(insect.children())
                .extracting(c -> c.name().value())
                .containsExactly("hesperiidae", "nymphalidae", "papilionidae", "pieridae");
    }

    @Test
    void buildByName_speciesName_hasNoChildren() {
        Insect insect = factory.buildByName(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name).orElseThrow();
        assertThat(insect.children()).isEmpty();
    }

    @Test
    void buildByName_composesRole_whenPresent() {
        // battus-philenor has a seeded SPECIES-rank InsectFunctionalRole
        // (guild KEYSTONE) in insect-functional-roles.json.
        Insect insect = factory.buildByName(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name).orElseThrow();

        assertThat(insect.role()).isNotNull();
        assertThat(insect.role().parentName()).isEqualTo(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);
    }

    @Test
    void buildByName_lifeStagesFollowMetabolySequence() {
        InsectSpeciesName name = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        List<LifeStage> reversed = nte.getNamed(InsectLifeStageTestEntitySource.class)
                .entityStream()
                .filter(stage -> stage.parentName().equals(name))
                .sorted(Comparator.comparingInt((LifeStage stage) -> stage.kind().ordinal()).reversed())
                .toList();
        assertThat(reversed).extracting(LifeStage::kind).first().isEqualTo(LifeStageKind.ADULT);

        Insect insect = factoryWithStages(reversed).buildByName(name).orElseThrow();

        assertThat(insect.lifeStages().stream().map(LifeStage::kind).toList())
                .containsExactly(LifeStageKind.EGG, LifeStageKind.LARVA, LifeStageKind.PUPA, LifeStageKind.ADULT);
    }

    /**
     * A hemimetabolous sequence is egg, nymph, adult: it differs from enum order once a larva is present, so only
     * the factory's {@code orderedBy(sequence)} produces this order, not repository order and not an ordinal sort.
     */
    @Test
    void buildByName_lifeStagesFollowTheSpeciesSequenceNotEnumOrder() {
        InsectSpeciesName name = TestInsectsIdentifiers.InsectSpecies.PeriplanetaAmericana.name;
        Description description = new Description(
                RandomValue.string(), RandomValue.string(), RandomValue.string(), RandomValue.string());
        List<LifeStage> repositoryOrder = List.of(
                new LarvaStage(LifeStageName.of(name, LifeStageKind.LARVA), name, null, null, null, description,
                        null, List.of(), List.of(), null, null),
                new AdultStage(LifeStageName.of(name, LifeStageKind.ADULT), name, null, null, null, description,
                        null, List.of(), null, null),
                new EggStage(LifeStageName.of(name, LifeStageKind.EGG), name, null, null, null, description,
                        null, null, null));

        Insect insect = factoryWithStages(repositoryOrder).buildByName(name).orElseThrow();

        assertThat(insect.lifeStages().stream().map(LifeStage::kind).toList())
                .containsExactly(LifeStageKind.EGG, LifeStageKind.ADULT, LifeStageKind.LARVA);
    }

    private InsectFactory factoryWithStages(List<LifeStage> stages) {
        InsectLifeStageQuery stub = () -> new InsectLifeStageQuery.LifeStageEntityQuery() {
            @Override
            public LifeStageCollection forParentName(InsectRankName parentName) {
                return LifeStageCollection.of(stages);
            }

            @Override
            public Optional<LifeStage> getByName(LifeStageName stageName) {
                return Optional.empty();
            }

            @Override
            public LifeStageCollection findByNameSet(Set<LifeStageName> names) {
                return LifeStageCollection.empty();
            }

            @Override
            public Page<LifeStage> findPage(PageRequest pageRequest) {
                return new Page<>(List.of(), pageRequest.pageNumber(), pageRequest.pageSize(), 0, false);
            }
        };
        return new InsectFactory(
                speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery,
                stub, citationQuery, featureQuery, roleQuery);
    }
}
