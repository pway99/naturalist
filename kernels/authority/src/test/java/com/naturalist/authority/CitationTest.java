package com.naturalist.authority;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CitationTest {

    private static final Observer observer = Observer.forClass(CitationTest.class);

    private static final AuthoritySource EOL = new AuthoritySource("eol", "Encyclopedia of Life");
    private static final AuthorityReference EOL_SWALLOWTAIL =
            new AuthorityReference(EOL, URI.create("https://eol.org/pages/1188585"));

    @Test
    void wellFormedOnlineSourcePassesInvariants() {
        Citation citation = new OnlineSource(
                CitationName.of("eol-battus-philenor-1188585"),
                EOL_SWALLOWTAIL,
                "Battus philenor — Encyclopedia of Life",
                "EOL Curators",
                2024,
                Instant.parse("2024-03-15T00:00:00Z"));

        InvariantObservation result = observer.forMethod("wellFormedOnlineSourcePassesInvariants")
                .namedEntity(citation, "citation");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void onlineSourceWithNullablesOmittedPassesInvariants() {
        Citation citation = new OnlineSource(
                CitationName.of("eol-battus-philenor-1188585"),
                EOL_SWALLOWTAIL,
                "Battus philenor — Encyclopedia of Life",
                null, null, null);

        InvariantObservation result = observer.forMethod("onlineSourceWithNullablesOmittedPassesInvariants")
                .namedEntity(citation, "citation");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullNameViolatesInvariants() {
        Citation citation = new OnlineSource(
                null,
                EOL_SWALLOWTAIL,
                "Battus philenor",
                null, null, null);

        InvariantObservation result = observer.forMethod("nullNameViolatesInvariants")
                .namedEntity(citation, "citation");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".name"));
    }

    @Test
    void nullAuthorityReferenceViolatesInvariants() {
        Citation citation = new OnlineSource(
                CitationName.of("eol-battus-philenor-1188585"),
                null,
                "Battus philenor",
                null, null, null);

        InvariantObservation result = observer.forMethod("nullAuthorityReferenceViolatesInvariants")
                .namedEntity(citation, "citation");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".authorityReference"));
    }

    @Test
    void blankTitleViolatesInvariants() {
        Citation citation = new OnlineSource(
                CitationName.of("eol-battus-philenor-1188585"),
                EOL_SWALLOWTAIL,
                "   ",
                null, null, null);

        InvariantObservation result = observer.forMethod("blankTitleViolatesInvariants")
                .namedEntity(citation, "citation");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".title"));
    }
}
