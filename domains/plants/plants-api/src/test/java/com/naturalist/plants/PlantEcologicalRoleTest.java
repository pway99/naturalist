package com.naturalist.plants;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlantEcologicalRoleTest {

    private static final Observer observer = Observer.forClass(PlantEcologicalRoleTest.class);

    @Test
    void speciesRankRoleIsValid() {
        MethodObserver mo = observer.forMethod("speciesRankRoleIsValid");
        PlantEcologicalRole role = new PlantEcologicalRole(
                PlantEcologicalRoleId.create(),
                PlantSpeciesName.of("aristolochia-californica"),
                Set.of(PlantRole.KEYSTONE_HOST, PlantRole.POLLINATOR_SUPPORT));

        InvariantObservation result = mo.observable(role, "role");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void genusRankRoleIsValid() {
        // The whole point of the cross-rank key: a bed of unidentified salvia still
        // supports pollinators, and the record can say so without inventing a species.
        MethodObserver mo = observer.forMethod("genusRankRoleIsValid");
        PlantEcologicalRole role = new PlantEcologicalRole(
                PlantEcologicalRoleId.create(),
                PlantGenusName.of("salvia"),
                Set.of(PlantRole.POLLINATOR_SUPPORT));

        InvariantObservation result = mo.observable(role, "role");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void familyRankRoleIsValid() {
        MethodObserver mo = observer.forMethod("familyRankRoleIsValid");
        PlantEcologicalRole role = new PlantEcologicalRole(
                PlantEcologicalRoleId.create(),
                PlantFamilyName.of("lamiaceae"),
                Set.of(PlantRole.POLLINATOR_SUPPORT));

        InvariantObservation result = mo.observable(role, "role");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void emptyRolesIsInvalid() {
        // A role record with no role has nothing to say. "Not characterised yet" is the
        // absence of a record, never a present record carrying an empty set.
        MethodObserver mo = observer.forMethod("emptyRolesIsInvalid");
        PlantEcologicalRole role = new PlantEcologicalRole(
                PlantEcologicalRoleId.create(),
                PlantSpeciesName.of("borago-officinalis"),
                Set.of());

        InvariantObservation result = mo.observable(role, "role");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".role.roles");
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        PlantEcologicalRole role = new PlantEcologicalRole(null, null, null);

        InvariantObservation result = mo.observable(role, "role");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".role.id", ".role.plantName", ".role.roles");
    }

    @Test
    void predicatesReadTheRoleSet() {
        PlantEcologicalRole keystone = new PlantEcologicalRole(
                PlantEcologicalRoleId.create(),
                PlantSpeciesName.of("aristolochia-californica"),
                Set.of(PlantRole.KEYSTONE_HOST));
        PlantEcologicalRole fixer = new PlantEcologicalRole(
                PlantEcologicalRoleId.create(),
                PlantSpeciesName.of("trifolium-repens"),
                Set.of(PlantRole.NITROGEN_FIXER, PlantRole.BENEFICIAL_INSECT_HABITAT));

        assertThat(keystone.isKeystoneHost()).isTrue();
        assertThat(keystone.isNitrogenFixer()).isFalse();
        assertThat(fixer.isNitrogenFixer()).isTrue();
        assertThat(fixer.supportsBiocontrolInsects()).isTrue();
        assertThat(fixer.playsRole(PlantRole.KEYSTONE_HOST)).isFalse();
    }
}
