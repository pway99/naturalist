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
                List.of(new InsectFeatureView.RankGroup(
                        InsectOrderName.of("lepidoptera"),
                        List.of(InsectFeature.of(InsectFeatureId.create(), "scaled wings")))));
        assertThat(mo.observable(view, "view").violations()).isEmpty();
    }

    @Test
    void nullComponents_reportInvariantViolations() {
        MethodObserver mo = observer.forMethod("nullComponents_reportInvariantViolations");
        InsectFeatureView view = new InsectFeatureView(null, null);
        assertThat(mo.observable(view, "view").violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".view.subject", ".view.groups");
    }

    @Test
    void validRankGroup_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validRankGroup_hasNoInvariantViolations");
        InsectFeatureView.RankGroup group = new InsectFeatureView.RankGroup(
                InsectFamilyName.of("papilionidae"),
                List.of(InsectFeature.of(InsectFeatureId.create(), "tailed hindwings")));
        assertThat(mo.observable(group, "rankGroup").violations()).isEmpty();
    }

    @Test
    void nullRankGroupComponents_reportInvariantViolations() {
        MethodObserver mo = observer.forMethod("nullRankGroupComponents_reportInvariantViolations");
        InsectFeatureView.RankGroup group = new InsectFeatureView.RankGroup(null, null);
        assertThat(mo.observable(group, "rankGroup").violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".rankGroup.rank", ".rankGroup.features");
    }
}
