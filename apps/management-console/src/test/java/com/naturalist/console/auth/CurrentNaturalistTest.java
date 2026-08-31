package com.naturalist.console.auth;

import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentNaturalistTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(NaturalistPrincipal principal) {
        var auth = new UsernamePasswordAuthenticationToken(
                principal, "n/a", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void displayName_forNaturalistPrincipal_returnsGivenName() {
        authenticateAs(new NaturalistPrincipal(
                NaturalistName.of("patrick-way"), "Patrick", "gerald.durrell@oakvista.example",
                "{bcrypt}x", false, true));

        assertThat(CurrentNaturalistView.displayName()).isEqualTo("Patrick");
        assertThat(CurrentNaturalistView.isAuthenticated()).isTrue();
    }

    @Test
    void displayName_whenNotNaturalist_isNull() {
        // no authentication set → admin/anonymous
        assertThat(CurrentNaturalistView.displayName()).isNull();
    }

    @Test
    void currentNaturalistName_forNaturalistPrincipal_isPresent() {
        authenticateAs(new NaturalistPrincipal(
                NaturalistName.of("patrick-way"), "Patrick", "gerald.durrell@oakvista.example",
                "{bcrypt}x", false, true));

        CurrentNaturalist seam = new SecurityContextCurrentNaturalist(name -> java.util.Optional.empty());
        assertThat(seam.name()).contains(NaturalistName.of("patrick-way"));
    }
}
