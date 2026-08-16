package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.biogeography.SacramentoValley;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicSpecies;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlantSpeciesTest {

    private static final Observer observer = Observer.forClass(PlantSpeciesTest.class);

    @Test
    void fullyPopulatedPlantIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedPlantIsValid");
        PlantSpecies plant = plant(PlantLifeForm.VINE);

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
                PlantGenusName.of("borago"),
                TaxonomicSpecies.of("officinalis"),
                description(),
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
                        ".plant.genusName",
                        ".plant.epithet",
                        ".plant.description",
                        ".plant.lifeForm",
                        ".plant.nativeBioregions",
                        ".plant.commonNames");
    }


    @Test
    void isNativeToReadsTheBioregionSet() {
        PlantSpecies plant = plant(PlantLifeForm.VINE);

        assertThat(plant.isNativeTo(new SacramentoValley())).isTrue();
    }

    private static PlantSpecies plant(PlantLifeForm lifeForm) {
        return new PlantSpecies(
                PlantSpeciesName.of("aristolochia-californica"),
                PlantGenusName.of("aristolochia"),
                TaxonomicSpecies.of("californica"),
                description(),
                lifeForm,
                Set.of(new SacramentoValley()),
                Set.of(CommonName.of("California Pipevine")));
    }


    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
