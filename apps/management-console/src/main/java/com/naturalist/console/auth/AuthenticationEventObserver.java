package com.naturalist.console.auth;

import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureCredentialsExpiredEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationFailureExpiredEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Observes every authentication attempt Spring Security decides. Boot publishes an
 * {@code AuthenticationSuccessEvent} or an {@code AbstractAuthenticationFailureEvent} for each
 * attempt through its {@code DefaultAuthenticationEventPublisher}; until now nothing listened,
 * so a credential-stuffing run against {@code /login} left no trace in the metrics.
 *
 * <p>Each outcome is a predicate-named constraint on an {@link Observer} in
 * {@link Observer.MonitoringMode#ALWAYS}, observed at {@link Level#WARN}: the violations are
 * the attack signal — {@code badCredentials} (password guessing),
 * {@code suspendedAccountAttempt} (a banned account trying to get back in),
 * {@code disabledAccountAttempt}, {@code expiredAccountAttempt},
 * {@code expiredCredentialsAttempt}, and the umbrella {@code authenticationFailed} — while the
 * same names with {@code valid=true} are the successful-login baseline those rates stand out
 * against. The provider hides "user not found" from the caller as bad credentials; whether the
 * email existed at all is observed upstream by {@link NaturalistUserDetailsService}, so the two
 * together separate email enumeration from password guessing.
 *
 * <p>Interactive success events (a duplicate of the success already counted) and logout events
 * are not attempts and are skipped, so the baseline counts each attempt once. No principal or
 * address is tagged — that is cardinality and PII a metric must not carry; <em>who</em> is a
 * question for an audit log, a separate decision.
 */
@Component
class AuthenticationEventObserver implements ApplicationListener<AbstractAuthenticationEvent> {
    private final Observer observer = Observer.forClass(getClass(), Observer.MonitoringMode.ALWAYS);

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        if (!(event instanceof AuthenticationSuccessEvent) && !(event instanceof AbstractAuthenticationFailureEvent)) {
            // InteractiveAuthenticationSuccessEvent duplicates the success already counted;
            // LogoutSuccessEvent is not an attempt. Neither belongs in the attempt baseline.
            return;
        }
        observer.forMethod("onApplicationEvent")
                .arguments(i -> i
                        .isFalse(event instanceof AbstractAuthenticationFailureEvent, "authenticationFailed")
                        .isFalse(event instanceof AuthenticationFailureBadCredentialsEvent, "badCredentials")
                        .isFalse(event instanceof AuthenticationFailureLockedEvent, "suspendedAccountAttempt")
                        .isFalse(event instanceof AuthenticationFailureDisabledEvent, "disabledAccountAttempt")
                        .isFalse(event instanceof AuthenticationFailureExpiredEvent, "expiredAccountAttempt")
                        .isFalse(event instanceof AuthenticationFailureCredentialsExpiredEvent, "expiredCredentialsAttempt"))
                .observe(Level.WARN);
    }
}
