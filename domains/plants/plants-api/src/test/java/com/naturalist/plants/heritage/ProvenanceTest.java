package com.naturalist.plants.heritage;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProvenanceTest {

    private static final Observer observer = Observer.forClass(ProvenanceTest.class);

    @Test
    void fullyPopulatedProvenanceIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedProvenanceIsValid");
        Provenance provenance = new Provenance(
                "Nick's family",
                "Calabria, Italy",
                50,
                "carried to California in the 1970s");

        InvariantObservation result = mo.observable(provenance, "provenance");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullSourceNotesIsValid() {
        MethodObserver mo = observer.forMethod("nullSourceNotesIsValid");
        Provenance provenance = new Provenance("Baker Creek", "Mansfield, Missouri", 3, null);

        InvariantObservation result = mo.observable(provenance, "provenance");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void blankOriginatorAndLocationAreInvalid() {
        MethodObserver mo = observer.forMethod("blankOriginatorAndLocationAreInvalid");
        Provenance provenance = new Provenance("  ", "", 0, null);

        InvariantObservation result = mo.observable(provenance, "provenance");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".provenance.originator",
                        ".provenance.originLocation");
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        Provenance provenance = new Provenance(null, null, 0, null);

        InvariantObservation result = mo.observable(provenance, "provenance");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".provenance.originator",
                        ".provenance.originLocation");
    }
}
