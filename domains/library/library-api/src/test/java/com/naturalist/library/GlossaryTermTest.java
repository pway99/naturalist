package com.naturalist.library;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlossaryTermTest {

    private static final Observer observer = Observer.forClass(GlossaryTermTest.class);

    @Test
    void validGlossaryTermPassesAllInvariants() {
        var mo = observer.forMethod("validGlossaryTermPassesAllInvariants");
        GlossaryTerm term = new GlossaryTerm(
                GlossaryTermName.of("conspicuous"),
                "Conspicuous",
                "Easily seen; standing out at a glance.",
                "Marks are ordered conspicuous to diagnostic.");

        InvariantObservation result = mo.namedEntity(term, "glossaryTerm");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullExampleIsPermitted() {
        var mo = observer.forMethod("nullExampleIsPermitted");
        GlossaryTerm term = new GlossaryTerm(
                GlossaryTermName.of("diagnostic"),
                "Diagnostic",
                "Decisive for identification; the clinching feature.",
                null);

        InvariantObservation result = mo.namedEntity(term, "glossaryTerm");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullRequiredComponentsProduceExpectedInvariantViolations() {
        var mo = observer.forMethod("nullRequiredComponentsProduceExpectedInvariantViolations");
        GlossaryTerm term = new GlossaryTerm(null, null, null, null);

        InvariantObservation result = mo.namedEntity(term, "glossaryTerm");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".glossaryTerm.name",
                        ".glossaryTerm.term",
                        ".glossaryTerm.definition");
    }
}
