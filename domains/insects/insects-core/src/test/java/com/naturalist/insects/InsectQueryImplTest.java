package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
    InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(imageRepository);
    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository, genusQuery);
    InsectQuery.FunctionalRoleQuery functionalRoleQuery = new FunctionalRoleQueryImpl(functionalRoleRepository);
    InsectQuery.OrderQuery orderQuery = new OrderQueryImpl(orderRepository);
    InsectQuery insectQuery = new InsectQueryImpl(
            speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery, orderQuery);

    @Test
    void accessors_returnNonNullDelegates() {
        assertThat(insectQuery.species()).isSameAs(speciesQuery);
        assertThat(insectQuery.images()).isSameAs(imageQuery);
        assertThat(insectQuery.families()).isSameAs(familyQuery);
        assertThat(insectQuery.genera()).isSameAs(genusQuery);
        assertThat(insectQuery.functionalRoles()).isSameAs(functionalRoleQuery);
        assertThat(insectQuery.orders()).isSameAs(orderQuery);
        assertThat(insectQuery.taxonView()).isNotNull();
    }

    @Test
    void accessors_idempotent() {
        assertThat(insectQuery.species()).isSameAs(insectQuery.species());
        assertThat(insectQuery.images()).isSameAs(insectQuery.images());
        assertThat(insectQuery.families()).isSameAs(insectQuery.families());
        assertThat(insectQuery.genera()).isSameAs(insectQuery.genera());
        assertThat(insectQuery.functionalRoles()).isSameAs(insectQuery.functionalRoles());
        assertThat(insectQuery.orders()).isSameAs(insectQuery.orders());
        assertThat(insectQuery.taxonView()).isSameAs(insectQuery.taxonView());
    }

    @Test
    void constructor_rejectsNullSpeciesQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(null, imageQuery, familyQuery, genusQuery, functionalRoleQuery, orderQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery");
    }

    @Test
    void constructor_rejectsNullImageQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, null, familyQuery, genusQuery, functionalRoleQuery, orderQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("imageQuery");
    }

    @Test
    void constructor_rejectsNullFamilyQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, null, genusQuery, functionalRoleQuery, orderQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("familyQuery");
    }

    @Test
    void constructor_rejectsNullGenusQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, familyQuery, null, functionalRoleQuery, orderQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("genusQuery");
    }

    @Test
    void constructor_rejectsNullFunctionalRoleQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, familyQuery, genusQuery, null, orderQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("functionalRoleQuery");
    }

    @Test
    void constructor_rejectsNullOrderQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("orderQuery");
    }

    @Test
    void constructor_collectsAllViolationsInSinglePass() {
        assertThatThrownBy(() -> new InsectQueryImpl(null, null, null, null, null, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery", "imageQuery", "familyQuery", "genusQuery", "functionalRoleQuery", "orderQuery");
    }
}
