package com.naturalist.console.auth;

import com.naturalist.challenge.ChallengeVerdict;
import com.naturalist.console.challenge.ChallengeGate;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * The bot challenge on {@code POST /login}, between {@link LoginRateLimitFilter} and Spring's login
 * filter (ADR-027 D7): the rate limits come first so a client over them costs no siteverify round
 * trip, and the challenge comes before the password check so a failed one burns no BCrypt round.
 *
 * <p>A {@code FAILED} verdict redirects to {@code /login?challenge} and {@code UNAVAILABLE} to
 * {@code /login?unavailable}, so the service failing closed (D6) reads differently from a wrong
 * password. Neither reaches the authentication observers, because the password was never checked;
 * {@link ChallengeGate} and the verifier observe the decision.
 *
 * <p>Constructed by {@code SecurityConfiguration} directly, like {@link LoginRateLimitFilter}, so it
 * sits in the security chain exactly once.
 */
public final class LoginChallengeFilter extends OncePerRequestFilter {
    /** The action the widget declares and the vendor echoes back. */
    private static final String ACTION = "login";

    private final ChallengeGate challenge;

    public LoginChallengeFilter(ChallengeGate challenge) {
        this.challenge = challenge;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (!LoginRateLimitFilter.LOGIN_ATTEMPT.matches(request)) {
            chain.doFilter(request, response);
            return;
        }
        ChallengeVerdict verdict = challenge.verify(request, ACTION);
        if (verdict.isPassed()) {
            chain.doFilter(request, response);
            return;
        }
        boolean unavailable = verdict.outcome() == ChallengeVerdict.Outcome.UNAVAILABLE;
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", request.getContextPath() + (unavailable ? "/login?unavailable" : "/login?challenge"));
    }
}
