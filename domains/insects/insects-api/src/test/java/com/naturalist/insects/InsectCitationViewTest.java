package com.naturalist.insects;

import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.CitationName;
import com.naturalist.authority.OnlineSource;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InsectCitationViewTest {

    private static final Observer observer = Observer.forClass(InsectCitationViewTest.class);

    private static OnlineSource citation(String slug, String title, String url) {
        return new OnlineSource(
                CitationName.of(slug),
                new AuthorityReference(
                        new AuthoritySource("eol", "Encyclopedia of Life"),
                    java.net.URI.create(url)),
                title, null, null, null);
    }

    @Test
    void validView_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validView_hasNoInvariantViolations");

        InsectCitationView view = new InsectCitationView(
                InsectSpeciesName.of("battus-philenor"),
                List.of(new InsectCitationView.RankedCitation(
                    citation("eol-lepidoptera-747", "Lepidoptera",
                        "https://eol.org/pages/747"),
                        InsectOrderName.of("lepidoptera"),
                        "EOL page")));

        InvariantObservation result = mo.observable(view, "view");
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponents_reportInvariantViolations() {
        MethodObserver mo = observer.forMethod("nullComponents_reportInvariantViolations");

        InsectCitationView view = new InsectCitationView(null, null);

        InvariantObservation result = mo.observable(view, "view");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".view.subject", ".view.citations");
    }

    @Test
    void validRankedCitation_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validRankedCitation_hasNoInvariantViolations");

        InsectCitationView.RankedCitation rc = new InsectCitationView.RankedCitation(
            citation("eol-battus-philenor-130502", "Battus philenor",
                "https://eol.org/pages/130502"),
                InsectFamilyName.of("papilionidae"),
                null);

        InvariantObservation result = mo.observable(rc, "rankedCitation");
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullRankedCitationComponents_reportInvariantViolations() {
        MethodObserver mo = observer.forMethod("nullRankedCitationComponents_reportInvariantViolations");

        InsectCitationView.RankedCitation rc = new InsectCitationView.RankedCitation(
                null, null, null);

        InvariantObservation result = mo.observable(rc, "rankedCitation");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".rankedCitation.citation", ".rankedCitation.attachedAt");
    }
}
