package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import com.naturalist.insects.lifestage.LifeStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectQueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);
    InsectImageRepositoryMock imageRepository = new InsectImageRepositoryMock(db);
    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    GenusRepositoryMock genusRepository = new GenusRepositoryMock(db);
    InsectFunctionalRoleRepositoryMock functionalRoleRepository = new InsectFunctionalRoleRepositoryMock(db);
    OrderRepositoryMock orderRepository = new OrderRepositoryMock(db);
    InsectFeatureRepositoryMock featureRepository = new InsectFeatureRepositoryMock(db);
    InsectFeatureAssignmentRepositoryMock featureAssignmentRepository = new InsectFeatureAssignmentRepositoryMock(db);
    FieldObservationRepositoryMock fieldObservationRepository = new FieldObservationRepositoryMock(db);
    InsectQuery.FieldObservationQuery fieldObservationQuery = new FieldObservationQueryImpl(fieldObservationRepository);
    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository, genusQuery);
    InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(
            imageRepository, speciesQuery, genusQuery, familyQuery);
    InsectQuery.FunctionalRoleQuery functionalRoleQuery = new FunctionalRoleQueryImpl(functionalRoleRepository);
    InsectQuery.OrderQuery orderQuery = new OrderQueryImpl(orderRepository);
    com.naturalist.library.CitationAssociationQuery citationAssociationQuery =
            new com.naturalist.library.CitationAssociationQuery() {
                @Override
                public com.naturalist.library.CitationAssociationCollection findByCitationName(
                        com.naturalist.authority.CitationName citationName) {
                    return com.naturalist.library.CitationAssociationCollection.empty();
                }

                @Override
                public com.naturalist.library.CitationAssociationCollection findBySubject(
                        com.naturalist.catalog.EntityRef subject) {
                    return com.naturalist.library.CitationAssociationCollection.empty();
                }
            };
    com.naturalist.library.CitationQuery libraryCitationQuery =
            new com.naturalist.library.CitationQuery() {
                @Override
                public java.util.Optional<com.naturalist.authority.Citation> getByName(
                        com.naturalist.authority.CitationName name) {
                    return java.util.Optional.empty();
                }

                @Override
                public com.naturalist.library.CitationCollection findByNameSet(
                        java.util.Set<com.naturalist.authority.CitationName> nameSet) {
                    return com.naturalist.library.CitationCollection.empty();
                }

                @Override
                public com.naturalist.data.Page<com.naturalist.authority.Citation> findPage(
                        com.naturalist.data.PageRequest pageRequest) {
                    return new com.naturalist.data.Page<>(
                            java.util.List.of(), pageRequest.pageNumber(), pageRequest.pageSize(), 0, false);
                }
            };
    InsectLifeStageQuery insectLifeStageQuery = new InsectLifeStageQuery() {
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
    InsectQuery insectQuery = new InsectQueryImpl(
            speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery,
            orderQuery, citationAssociationQuery, libraryCitationQuery, insectLifeStageQuery,
            featureRepository, featureAssignmentRepository, fieldObservationQuery);

    @Test
    void accessors_returnNonNullDelegates() {
        assertThat(insectQuery.species()).isSameAs(speciesQuery);
        assertThat(insectQuery.images()).isSameAs(imageQuery);
        assertThat(insectQuery.fieldObservations()).isSameAs(fieldObservationQuery);
        assertThat(insectQuery.families()).isSameAs(familyQuery);
        assertThat(insectQuery.genera()).isSameAs(genusQuery);
        assertThat(insectQuery.functionalRoles()).isSameAs(functionalRoleQuery);
        assertThat(insectQuery.orders()).isSameAs(orderQuery);
        assertThat(insectQuery.taxonView()).isNotNull();
        assertThat(insectQuery.citations()).isNotNull();
        assertThat(insectQuery.features()).isNotNull();
    }

    @Test
    void accessors_idempotent() {
        assertThat(insectQuery.species()).isSameAs(insectQuery.species());
        assertThat(insectQuery.images()).isSameAs(insectQuery.images());
        assertThat(insectQuery.fieldObservations()).isSameAs(insectQuery.fieldObservations());
        assertThat(insectQuery.families()).isSameAs(insectQuery.families());
        assertThat(insectQuery.genera()).isSameAs(insectQuery.genera());
        assertThat(insectQuery.functionalRoles()).isSameAs(insectQuery.functionalRoles());
        assertThat(insectQuery.orders()).isSameAs(insectQuery.orders());
        assertThat(insectQuery.taxonView()).isSameAs(insectQuery.taxonView());
        assertThat(insectQuery.citations()).isSameAs(insectQuery.citations());
        assertThat(insectQuery.features()).isSameAs(insectQuery.features());
    }

    @Test
    void constructor_rejectsNullSpeciesQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(null, imageQuery, familyQuery, genusQuery, functionalRoleQuery, orderQuery, citationAssociationQuery, libraryCitationQuery, insectLifeStageQuery, featureRepository, featureAssignmentRepository, fieldObservationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery");
    }

    @Test
    void constructor_rejectsNullImageQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, null, familyQuery, genusQuery, functionalRoleQuery, orderQuery, citationAssociationQuery, libraryCitationQuery, insectLifeStageQuery, featureRepository, featureAssignmentRepository, fieldObservationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("imageQuery");
    }

    @Test
    void constructor_rejectsNullFamilyQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, null, genusQuery, functionalRoleQuery, orderQuery, citationAssociationQuery, libraryCitationQuery, insectLifeStageQuery, featureRepository, featureAssignmentRepository, fieldObservationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("familyQuery");
    }

    @Test
    void constructor_rejectsNullGenusQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, familyQuery, null, functionalRoleQuery, orderQuery, citationAssociationQuery, libraryCitationQuery, insectLifeStageQuery, featureRepository, featureAssignmentRepository, fieldObservationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("genusQuery");
    }

    @Test
    void constructor_rejectsNullFunctionalRoleQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, familyQuery, genusQuery, null, orderQuery, citationAssociationQuery, libraryCitationQuery, insectLifeStageQuery, featureRepository, featureAssignmentRepository, fieldObservationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("functionalRoleQuery");
    }

    @Test
    void constructor_rejectsNullOrderQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery, null, citationAssociationQuery, libraryCitationQuery, insectLifeStageQuery, featureRepository, featureAssignmentRepository, fieldObservationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("orderQuery");
    }

    @Test
    void constructor_rejectsNullCitationAssociationQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery,
                functionalRoleQuery, orderQuery, null, libraryCitationQuery, insectLifeStageQuery,
                featureRepository, featureAssignmentRepository, fieldObservationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("citationAssociationQuery");
    }

    @Test
    void constructor_rejectsNullLibraryCitationQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery,
                functionalRoleQuery, orderQuery, citationAssociationQuery, null, insectLifeStageQuery,
                featureRepository, featureAssignmentRepository, fieldObservationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("libraryCitationQuery");
    }

    @Test
    void constructor_rejectsNullInsectLifeStageQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery,
                functionalRoleQuery, orderQuery, citationAssociationQuery, libraryCitationQuery, null,
                featureRepository, featureAssignmentRepository, fieldObservationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("insectLifeStageQuery");
    }

    @Test
    void constructor_rejectsNullFieldObservationQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery,
                functionalRoleQuery, orderQuery, citationAssociationQuery, libraryCitationQuery, insectLifeStageQuery,
                featureRepository, featureAssignmentRepository, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("fieldObservationQuery");
    }

    @Test
    void constructor_collectsAllViolationsInSinglePass() {
        assertThatThrownBy(() -> new InsectQueryImpl(null, null, null, null, null, null, null, null, null, null, null, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery", "imageQuery", "familyQuery",
                        "genusQuery", "functionalRoleQuery", "orderQuery",
                        "citationAssociationQuery", "libraryCitationQuery", "insectLifeStageQuery",
                        "featureRepository", "featureAssignmentRepository", "fieldObservationQuery");
    }

    @Test
    void getByName_delegatesToFactory() {
        Optional<Insect> result = insectQuery.getByName(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);
        assertThat(result).isPresent();
        Insect insect = result.get();
        assertThat(insect.speciesName()).hasValue(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);
        assertThat(insect.genusName()).isPresent();
        assertThat(insect.familyName()).isPresent();
        assertThat(insect.orderName()).isPresent();
    }
}
