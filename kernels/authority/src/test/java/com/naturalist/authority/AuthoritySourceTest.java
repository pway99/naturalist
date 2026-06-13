package com.naturalist.authority;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthoritySourceTest {

    private static final Observer observer = Observer.forClass(AuthoritySourceTest.class);

    @Test
    void wellFormedSourcePassesInvariants() {
        AuthoritySource source = new AuthoritySource("eol", "Encyclopedia of Life");

        InvariantObservation result = observer.forMethod("wellFormedSourcePassesInvariants")
                .observable(source, "source");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void blankIdViolatesInvariants() {
        AuthoritySource source = new AuthoritySource("  ", "Encyclopedia of Life");

        InvariantObservation result = observer.forMethod("blankIdViolatesInvariants")
                .observable(source, "source");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".id"));
    }

    @Test
    void blankDisplayNameViolatesInvariants() {
        AuthoritySource source = new AuthoritySource("eol", "  ");

        InvariantObservation result = observer.forMethod("blankDisplayNameViolatesInvariants")
                .observable(source, "source");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".displayName"));
    }
}