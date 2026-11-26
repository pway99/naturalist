package com.naturalist.console.auth;

import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountSessionsTest {

    private final SessionRegistryImpl registry = new SessionRegistryImpl();
    private final AccountSessions sessions = new AccountSessions(registry);

    private static UserDetails principal(String username) {
        return User.withUsername(username).password("not-a-real-secret").roles("NATURALIST").build();
    }

    private String signIn(UserDetails principal) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        sessions.register(request, principal);
        return request.getSession().getId();
    }

    @Test
    void endAll_expiresEverySessionOfTheUsername_acrossSeparatelyBuiltPrincipals() {
        // Two logins of one account build two principal objects; both sessions must end.
        String first = signIn(principal("ada@example.com"));
        String second = signIn(principal("ada@example.com"));
        String other = signIn(principal("grace@example.com"));

        assertThat(sessions.endAll("ada@example.com")).isEqualTo(2);

        assertThat(registry.getSessionInformation(first).isExpired()).isTrue();
        assertThat(registry.getSessionInformation(second).isExpired()).isTrue();
        SessionInformation untouched = registry.getSessionInformation(other);
        assertThat(untouched.isExpired()).isFalse();
    }

    @Test
    void endAll_returnsZero_whenTheAccountIsSignedInNowhere() {
        signIn(principal("grace@example.com"));

        assertThat(sessions.endAll("ada@example.com")).isZero();
    }

    @Test
    void endAll_refusesABlankUsername() {
        assertThatThrownBy(() -> sessions.endAll(" "))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("username");
    }
}
