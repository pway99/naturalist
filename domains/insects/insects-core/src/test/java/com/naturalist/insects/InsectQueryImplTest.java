package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectQueryImplTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Wired InsectQuery insectQuery;
    @Wired InsectQuery.SpeciesQuery speciesQuery;
    @Wired InsectQuery.ImageQuery imageQuery;
    @Wired InsectQuery.FamilyQuery familyQuery;
    @Wired InsectQuery.GenusQuery genusQuery;
    @Wired InsectQuery.FunctionalRoleQuery functionalRoleQuery;
    @Wired InsectQuery.OrderQuery orderQuery;
    @Wired InsectQuery.ObservationQuery observationQuery;
    @Wired InsectCitationQueryImpl citationQuery;
    @Wired InsectFeatureQueryImpl featureQuery;
    @Wired InsectLifeStageQuery insectLifeStageQuery;

    @Test
    void accessors_returnNonNullDelegates() {
        assertThat(insectQuery.species()).isSameAs(speciesQuery);
        assertThat(insectQuery.images()).isSameAs(imageQuery);
        assertThat(insectQuery.observations()).isSameAs(observationQuery);
        assertThat(insectQuery.families()).isSameAs(familyQuery);
        assertThat(insectQuery.genera()).isSameAs(genusQuery);
        assertThat(insectQuery.functionalRoles()).isSameAs(functionalRoleQuery);
        assertThat(insectQuery.orders()).isSameAs(orderQuery);
        assertThat(insectQuery.citations()).isNotNull();
        assertThat(insectQuery.features()).isNotNull();
    }

    @Test
    void accessors_idempotent() {
        assertThat(insectQuery.species()).isSameAs(insectQuery.species());
        assertThat(insectQuery.images()).isSameAs(insectQuery.images());
        assertThat(insectQuery.observations()).isSameAs(insectQuery.observations());
        assertThat(insectQuery.families()).isSameAs(insectQuery.families());
        assertThat(insectQuery.genera()).isSameAs(insectQuery.genera());
        assertThat(insectQuery.functionalRoles()).isSameAs(insectQuery.functionalRoles());
        assertThat(insectQuery.orders()).isSameAs(insectQuery.orders());
        assertThat(insectQuery.citations()).isSameAs(insectQuery.citations());
        assertThat(insectQuery.features()).isSameAs(insectQuery.features());
    }

    @Test
    void constructor_rejectsNullSpeciesQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(null, imageQuery, familyQuery, genusQuery, functionalRoleQuery, orderQuery, citationQuery, featureQuery, insectLifeStageQuery, observationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery");
    }

    @Test
    void constructor_rejectsNullImageQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, null, familyQuery, genusQuery, functionalRoleQuery, orderQuery, citationQuery, featureQuery, insectLifeStageQuery, observationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("imageQuery");
    }

    @Test
    void constructor_rejectsNullFamilyQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, null, genusQuery, functionalRoleQuery, orderQuery, citationQuery, featureQuery, insectLifeStageQuery, observationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("familyQuery");
    }

    @Test
    void constructor_rejectsNullGenusQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, familyQuery, null, functionalRoleQuery, orderQuery, citationQuery, featureQuery, insectLifeStageQuery, observationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("genusQuery");
    }

    @Test
    void constructor_rejectsNullFunctionalRoleQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, familyQuery, genusQuery, null, orderQuery, citationQuery, featureQuery, insectLifeStageQuery, observationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("functionalRoleQuery");
    }

    @Test
    void constructor_rejectsNullOrderQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery, null, citationQuery, featureQuery, insectLifeStageQuery, observationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("orderQuery");
    }

    @Test
    void constructor_rejectsNullCitationQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery,
                functionalRoleQuery, orderQuery, null, featureQuery, insectLifeStageQuery, observationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("citationQuery");
    }

    @Test
    void constructor_rejectsNullFeatureQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery,
                functionalRoleQuery, orderQuery, citationQuery, null, insectLifeStageQuery, observationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("featureQuery");
    }

    @Test
    void constructor_rejectsNullInsectLifeStageQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery,
                functionalRoleQuery, orderQuery, citationQuery, featureQuery, null, observationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("insectLifeStageQuery");
    }

    @Test
    void constructor_rejectsNullInsectObservationQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery,
                functionalRoleQuery, orderQuery, citationQuery, featureQuery, insectLifeStageQuery, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("observationQuery");
    }

    @Test
    void constructor_collectsAllViolationsInSinglePass() {
        assertThatThrownBy(() -> new InsectQueryImpl(null, null, null, null, null, null, null, null, null, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery", "imageQuery", "familyQuery",
                        "genusQuery", "functionalRoleQuery", "orderQuery",
                        "citationQuery", "featureQuery", "insectLifeStageQuery",
                        "observationQuery");
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
