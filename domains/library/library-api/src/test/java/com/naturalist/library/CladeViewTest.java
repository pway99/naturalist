package com.naturalist.library;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CladeViewTest {

    private static final Observer observer = Observer.forClass(CladeViewTest.class);

    @Test
    void validViewPassesAllInvariants() {
        var mo = observer.forMethod("validViewPassesAllInvariants");
        var view = new CladeView(
                new CladeStep("lepidoptera", "Lepidoptera", Optional.of(LinealRank.ORDER)),
                List.of(
                        new CladeStep("eukaryota", "Eukaryota", Optional.empty()),
                        new CladeStep("animalia", "Animalia", Optional.of(LinealRank.KINGDOM))),
                List.of(
                        new CladeStep("papilionoidea", "Papilionoidea", Optional.empty())));

        InvariantObservation result = mo.observable(view, "view");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponentsProduceExpectedViolations() {
        var mo = observer.forMethod("nullComponentsProduceExpectedViolations");
        var view = new CladeView(null, null, null);

        InvariantObservation result = mo.observable(view, "view");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".view.subject",
                        ".view.ancestry",
                        ".view.children");
    }
}
