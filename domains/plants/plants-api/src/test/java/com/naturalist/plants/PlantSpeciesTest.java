package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.biogeography.SacramentoValley;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import com.naturalist.taxonomy.TaxonomicSpecies;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlantSpeciesTest {

    private static final Observer observer = Observer.forClass(PlantSpeciesTest.class);

    @Test
    void fullyPopulatedPlantIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedPlantIsValid");
        PlantSpecies plant = plant(Set.of(PlantRole.KEYSTONE_HOST), PlantLifeForm.VINE);

        InvariantObservation result = mo.namedEntity(plant, "plant");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void emptyBioregionsAndCommonNamesAreValid() {
        // An empty nativeBioregions set means "no asserted native range", not
        // "unknown" — it is a legal encoding, not a missing value. Same for
        // commonNames: "no asserted vernacular name yet".
        MethodObserver mo = observer.forMethod("emptyBioregionsAndCommonNamesAreValid");
        PlantSpecies plant = new PlantSpecies(
                PlantSpeciesName.of("borago-officinalis"),
                taxonomy(),
                description(),
                Set.of(PlantRole.BENEFICIAL_INSECT_HABITAT),
                PlantLifeForm.ANNUAL,
                Set.of(),
                Set.of());

        InvariantObservation result = mo.namedEntity(plant, "plant");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        PlantSpecies plant = new PlantSpecies(null, null, null, null, null, null, null);

        InvariantObservation result = mo.namedEntity(plant, "plant");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".plant.name",
                        ".plant.taxonomy",
                        ".plant.description",
                        ".plant.roles",
                        ".plant.lifeForm",
                        ".plant.nativeBioregions",
                        ".plant.commonNames");
    }

    @Test
    void keystoneHostPredicateReadsTheRoleSet() {
        PlantSpecies keystone = plant(Set.of(PlantRole.KEYSTONE_HOST), PlantLifeForm.VINE);
        PlantSpecies notKeystone = plant(Set.of(PlantRole.FOOD_CROP), PlantLifeForm.ANNUAL);

        assertThat(keystone.isKeystoneHost()).isTrue();
        assertThat(notKeystone.isKeystoneHost()).isFalse();
    }

    @Test
    void isNativeToReadsTheBioregionSet() {
        PlantSpecies plant = plant(Set.of(PlantRole.KEYSTONE_HOST), PlantLifeForm.VINE);

        assertThat(plant.isNativeTo(new SacramentoValley())).isTrue();
    }

    private static PlantSpecies plant(Set<PlantRole> roles, PlantLifeForm lifeForm) {
        return new PlantSpecies(
                PlantSpeciesName.of("aristolochia-californica"),
                taxonomy(),
                description(),
                roles,
                lifeForm,
                Set.of(new SacramentoValley()),
                Set.of(CommonName.of("California Pipevine")));
    }

    private static TaxonomicClassification taxonomy() {
        return new TaxonomicClassification(
                TaxonomicOrder.of("Piperales"),
                TaxonomicFamily.of("Aristolochiaceae"),
                TaxonomicGenus.of("Aristolochia"),
                TaxonomicSpecies.of("californica"));
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
