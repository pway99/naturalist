package com.naturalist.plants;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantFeatureTest {

    private static final Observer observer = Observer.forClass(PlantFeatureTest.class);

    @Test
    void validFeature_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validFeature_hasNoInvariantViolations");
        PlantFeature f = PlantFeature.of(PlantFeatureId.create(), "opposite leaves");

        InvariantObservation result = mo.observable(f, "feature");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void valueIsTrimmedAndLowercased() {
        assertThat(PlantFeature.of(PlantFeatureId.create(), "  Ray Florets ").value())
                .isEqualTo("ray florets");
    }

    @Test
    void nullComponents_reportViolations() {
        MethodObserver mo = observer.forMethod("nullComponents_reportViolations");
        PlantFeature f = new PlantFeature(null, null);

        InvariantObservation result = mo.observable(f, "feature");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".feature.id");
    }
}
