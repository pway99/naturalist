package com.naturalist.authority;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorityReferenceTest {

    private static final Observer observer = Observer.forClass(AuthorityReferenceTest.class);
    private static final AuthoritySource EOL = new AuthoritySource("eol", "Encyclopedia of Life");

    @Test
    void wellFormedReferencePassesInvariants() {
        AuthorityReference ref = new AuthorityReference(EOL, URI.create("https://eol.org/pages/1188585"));

        InvariantObservation result = observer.forMethod("wellFormedReferencePassesInvariants")
                .observable(ref, "ref");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullSourceViolatesInvariants() {
        AuthorityReference ref = new AuthorityReference(null, URI.create("https://eol.org/pages/1188585"));

        InvariantObservation result = observer.forMethod("nullSourceViolatesInvariants")
                .observable(ref, "ref");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".source"));
    }

    @Test
    void nullUrlViolatesInvariants() {
        AuthorityReference ref = new AuthorityReference(EOL, null);

        InvariantObservation result = observer.forMethod("nullUrlViolatesInvariants")
                .observable(ref, "ref");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".url"));
    }
}