package com.naturalist.insects;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InsectFeatureViewTest {

    private static final Observer observer = Observer.forClass(InsectFeatureViewTest.class);

    @Test
    void validView_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validView_hasNoInvariantViolations");

        InsectFeatureView view = new InsectFeatureView(
                InsectSpeciesName.of("battus-philenor"),
                List.of(new InsectFeatureView.RankedFeature(
                        InsectFeature.of(InsectFeatureId.create(), "scaled wings"),
                        InsectOrderName.of("lepidoptera"),
                        0)));

        InvariantObservation result = mo.observable(view, "view");
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponents_reportInvariantViolations() {
        MethodObserver mo = observer.forMethod("nullComponents_reportInvariantViolations");

        InsectFeatureView view = new InsectFeatureView(null, null);

        InvariantObservation result = mo.observable(view, "view");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".view.subject", ".view.features");
    }

    @Test
    void validRankedFeature_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validRankedFeature_hasNoInvariantViolations");

        InsectFeatureView.RankedFeature rf = new InsectFeatureView.RankedFeature(
                InsectFeature.of(InsectFeatureId.create(), "tailed hindwings"),
                InsectFamilyName.of("papilionidae"),
                1);

        InvariantObservation result = mo.observable(rf, "rankedFeature");
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullRankedFeatureComponents_reportInvariantViolations() {
        MethodObserver mo = observer.forMethod("nullRankedFeatureComponents_reportInvariantViolations");

        InsectFeatureView.RankedFeature rf = new InsectFeatureView.RankedFeature(
                null, null, 0);

        InvariantObservation result = mo.observable(rf, "rankedFeature");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".rankedFeature.feature", ".rankedFeature.assignedAt");
    }
}
