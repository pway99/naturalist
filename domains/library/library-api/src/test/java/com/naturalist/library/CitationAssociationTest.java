package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CitationAssociationTest {

    private static final Observer observer = Observer.forClass(CitationAssociationTest.class);

    @Test
    void validCitationAssociation_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validCitationAssociation_hasNoInvariantViolations");

        CitationAssociation association = new CitationAssociation(
                CitationAssociationId.create(),
                CitationName.of("eol-battus-philenor-130502"),
                new EntityRef(new TestDomainId(), CitationName.of("some-entity")),
                null);

        InvariantObservation result = mo.observable(association, "association");
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponents_reportInvariantViolations() {
        MethodObserver mo = observer.forMethod("nullComponents_reportInvariantViolations");

        CitationAssociation association = new CitationAssociation(null, null, null, null);

        InvariantObservation result = mo.observable(association, "association");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".association.name", ".association.citationName", ".association.subject");
    }

    /** Minimal DomainId for test isolation — no dependency on any real domain. */
    private record TestDomainId() implements DomainId {
        @Override
        public String value() {
            return "test";
        }
    }
}
