package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlantGenusTest {

    private static final Observer observer = Observer.forClass(PlantGenusTest.class);

    @Test
    void fullyPopulatedGenusIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedGenusIsValid");

        InvariantObservation result = mo.namedEntity(thymus(), "genus");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void familyNameIsRequired() {
        // The upward typed FK is what makes a genus record navigable — a genus
        // with no parent family is not a partial record, it is an invalid one.
        MethodObserver mo = observer.forMethod("familyNameIsRequired");
        PlantGenus genus = new PlantGenus(
                PlantGenusName.of("thymus"),
                null,
                TaxonomicFamily.of("Lamiaceae"),
                TaxonomicGenus.of("Thymus"),
                description(),
                Set.of());

        InvariantObservation result = mo.namedEntity(genus, "genus");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".genus.familyName");
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        PlantGenus genus = new PlantGenus(null, null, null, null, null, null);

        InvariantObservation result = mo.namedEntity(genus, "genus");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".genus.name",
                        ".genus.familyName",
                        ".genus.family",
                        ".genus.genus",
                        ".genus.description",
                        ".genus.commonNames");
    }

    private static PlantGenus thymus() {
        return new PlantGenus(
                PlantGenusName.of("thymus"),
                PlantFamilyName.of("lamiaceae"),
                TaxonomicFamily.of("Lamiaceae"),
                TaxonomicGenus.of("Thymus"),
                description(),
                Set.of(CommonName.of("thyme")));
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
