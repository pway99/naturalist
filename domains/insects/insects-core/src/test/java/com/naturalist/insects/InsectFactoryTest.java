package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.insects.lifestage.LifeStage;
import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.library.CitationQuery;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectFactoryTest {

    static final Observer observer = Observer.forClass(InsectFactoryTest.class);

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectFamilyRepositoryMock familyRepository = new InsectFamilyRepositoryMock(db);
    InsectGenusRepositoryMock genusRepository = new InsectGenusRepositoryMock(db);
    InsectSpeciesRepositoryMock speciesRepository = new InsectSpeciesRepositoryMock(db);
    InsectImageRepositoryMock imageRepository = new InsectImageRepositoryMock(db);
    InsectOrderRepositoryMock orderRepository = new InsectOrderRepositoryMock(db);

    InsectQuery.FamilyQuery familyQuery = new InsectFamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new InsectGenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new InsectSpeciesQueryImpl(speciesRepository, genusQuery);
    InsectQuery.ImageQuery imageQuery = new InsectImageQueryImpl(
            imageRepository, speciesQuery, genusQuery, familyQuery);
    InsectQuery.OrderQuery orderQuery = new InsectOrderQueryImpl(orderRepository);

    LibraryTestContext libraryContext = LibraryTestContext.create(db);
    CitationAssociationQuery citationAssociationQuery = libraryContext.citationAssociationQuery();
    CitationQuery libraryCitationQuery = libraryContext.citationQuery();

    InsectAncestryResolver ancestryResolver = new InsectAncestryResolver(speciesQuery, genusQuery, familyQuery);
    InsectQuery.CitationQuery citationQuery = new InsectCitationQueryImpl(
            citationAssociationQuery, libraryCitationQuery, ancestryResolver);

    InsectFeatureRepositoryMock featureRepository = new InsectFeatureRepositoryMock(db);
    InsectFeatureAssignmentRepositoryMock assignmentRepository =
            new InsectFeatureAssignmentRepositoryMock(db);
    InsectQuery.FeatureQuery featureQuery = new InsectFeatureQueryImpl(
            featureRepository, assignmentRepository, ancestryResolver);

    InsectFunctionalRoleRepositoryMock functionalRoleRepository = new InsectFunctionalRoleRepositoryMock(db);
    InsectQuery.FunctionalRoleQuery roleQuery = new InsectFunctionalRoleQueryImpl(functionalRoleRepository);

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

    InsectFactory factory = new InsectFactory(
            speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery,
            lifeStageQuery, citationQuery, featureQuery, roleQuery);

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
                assertThat(c).isInstanceOf(InsectFamilyView.class));
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
}
