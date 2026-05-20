package com.naturalist.insects;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectFunctionalRoleTest {

    private static final Observer observer = Observer.forClass(InsectFunctionalRoleTest.class);

    @Test
    void roleWithSpeciesParentAndPopulatedGuildsIsValid() {
        MethodObserver mo = observer.forMethod("roleWithSpeciesParentAndPopulatedGuildsIsValid");
        InsectFunctionalRole role = new InsectFunctionalRole(
                InsectFunctionalRoleId.create(),
                InsectSpeciesName.of("battus-philenor"),
                Set.of(FunctionalGuild.KEYSTONE, FunctionalGuild.POLLINATOR),
                true);

        InvariantObservation result = mo.observable(role, "role");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void roleWithGenusParentIsValid() {
        // Cross-rank: a genus-level role assignment is the whole point of
        // this entity. The same record shape, just a different InsectRankName
        // permit on parentName.
        MethodObserver mo = observer.forMethod("roleWithGenusParentIsValid");
        InsectFunctionalRole role = new InsectFunctionalRole(
                InsectFunctionalRoleId.create(),
                InsectGenusName.of("empoasca"),
                Set.of(FunctionalGuild.FOOD_WEB),
                false);

        InvariantObservation result = mo.observable(role, "role");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void roleWithFamilyParentIsValid() {
        MethodObserver mo = observer.forMethod("roleWithFamilyParentIsValid");
        InsectFunctionalRole role = new InsectFunctionalRole(
                InsectFunctionalRoleId.create(),
                InsectFamilyName.of("syrphidae"),
                Set.of(FunctionalGuild.POLLINATOR, FunctionalGuild.PREDATOR),
                true);

        InvariantObservation result = mo.observable(role, "role");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void emptyGuildsIsInvalid() {
        // A role record with no guild assignment has no purpose — the whole
        // point of the entity is to carry the structured ecological role.
        // "Not yet documented" is expressed by the absence of the role
        // record, not by an empty set on a present record.
        MethodObserver mo = observer.forMethod("emptyGuildsIsInvalid");
        InsectFunctionalRole role = new InsectFunctionalRole(
                InsectFunctionalRoleId.create(),
                InsectSpeciesName.of("tachinid-fly"),
                Set.of(),
                false);

        InvariantObservation result = mo.observable(role, "role");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".role.guilds");
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        InsectFunctionalRole role = new InsectFunctionalRole(null, null, null, false);

        InvariantObservation result = mo.observable(role, "role");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".role.name", ".role.parentName", ".role.guilds");
    }
}
