package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicOrder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlantFamilyTest {

    private static final Observer observer = Observer.forClass(PlantFamilyTest.class);

    @Test
    void fullyPopulatedFamilyIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedFamilyIsValid");
        PlantFamily family = new PlantFamily(
                PlantFamilyName.of("lamiaceae"),
                TaxonomicOrder.of("Lamiales"),
                TaxonomicFamily.of("Lamiaceae"),
                description(),
                Set.of(CommonName.of("mint family")));

        InvariantObservation result = mo.namedEntity(family, "family");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void emptyCommonNamesIsValid() {
        MethodObserver mo = observer.forMethod("emptyCommonNamesIsValid");
        PlantFamily family = new PlantFamily(
                PlantFamilyName.of("aristolochiaceae"),
                TaxonomicOrder.of("Piperales"),
                TaxonomicFamily.of("Aristolochiaceae"),
                description(),
                Set.of());

        InvariantObservation result = mo.namedEntity(family, "family");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        PlantFamily family = new PlantFamily(null, null, null, null, null);

        InvariantObservation result = mo.namedEntity(family, "family");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".family.name",
                        ".family.order",
                        ".family.family",
                        ".family.description",
                        ".family.commonNames");
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
