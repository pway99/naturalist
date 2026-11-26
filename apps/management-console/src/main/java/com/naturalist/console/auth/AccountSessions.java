package com.naturalist.console.auth;

import com.naturalist.observability.Observer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The live sessions of signed-in accounts, held in Spring's {@link SessionRegistry}. A form login
 * registers its session through the security chain's session management; a programmatic login
 * (registration's auto-login) registers through {@link #register}. {@link #endAll} expires every
 * live session of one login: the {@code ConcurrentSessionFilter} then logs each one out on its
 * next request. An admin suspend calls it so the ban takes effect at once, and a password reset
 * (launch 5.4) is meant to call it so a session an attacker holds does not outlive the credential
 * change.
 *
 * <p>Sessions are matched by {@link UserDetails#getUsername()}, the login email, rather than by
 * principal equality: a principal is rebuilt on every login and on verification, so two
 * principals of one account are never the same object.
 */
@Component
public class AccountSessions {

    private static final Observer observer = Observer.forClass(AccountSessions.class, Observer.MonitoringMode.ALWAYS);

    private final SessionRegistry sessionRegistry;

    AccountSessions(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /** Records the request's session as a live session of {@code principal}; call after any session-id rotation. */
    public void register(HttpServletRequest request, UserDetails principal) {
        observer.arguments("register", i -> i
                .notNull(request, "request")
                .notNull(principal, "principal"))
                .throwWhenInvalid();
        sessionRegistry.registerNewSession(request.getSession().getId(), principal);
    }

    /**
     * Expires every live session whose principal's username is {@code username} (an account's
     * login email). Returns how many were expired; zero when the account is signed in nowhere.
     */
    public int endAll(String username) {
        observer.arguments("endAll", i -> i.notBlank(username, "username"))
                .throwWhenInvalid();
        int ended = 0;
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UserDetails user && username.equals(user.getUsername())) {
                List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                for (SessionInformation session : sessions) {
                    session.expireNow();
                    ended++;
                }
            }
        }
        return ended;
    }
}
