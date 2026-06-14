package com.naturalist.library;

import com.naturalist.RandomValue;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConceptTest {

    private static final Observer observer = Observer.forClass(ConceptTest.class);

    @Test
    void validConceptPassesAllInvariants() {
        var mo = observer.forMethod("validConceptPassesAllInvariants");
        Concept concept = new Concept(
                ConceptName.of("clade"),
                "What is a clade?",
                description());

        InvariantObservation result = mo.namedEntity(concept, "concept");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponentsProduceExpectedInvariantViolations() {
        var mo = observer.forMethod("nullComponentsProduceExpectedInvariantViolations");
        Concept concept = new Concept(null, null, null);

        InvariantObservation result = mo.namedEntity(concept, "concept");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".concept.name",
                        ".concept.title",
                        ".concept.description");
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
