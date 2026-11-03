package com.naturalist.console.auth;

import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.RateLimitExceededException;
import com.naturalist.resilience.RateLimiter;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Resilient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * A per-client ceiling on identification, ahead of the controllers: every
 * {@code POST /<domain>/identify} takes one {@code vision.identification.client} permit for the
 * request's {@link ClientKey} -- two a minute per address, shared across the organism domains --
 * and past it is redirected ({@code 303 See Other}) to {@code /<domain>/identify?limit=RATE}
 * without reaching the controller, the budget or the vendor (ADR-028 spend layer 4). A redirect,
 * not a {@code 429}: the upload script submits by {@code fetch}, follows a redirect, and on any
 * other answer silently reloads the form; the identify page already words {@code RATE}.
 *
 * <p>The path is matched by Spring Security's {@link PathPatternRequestMatcher}, against the
 * path within the application as both the container and MockMvc present it, never against
 * {@code getServletPath()}, which MockMvc leaves empty.
 *
 * <p>Two a minute is deliberately below the global {@code vision.identification} three a
 * minute, so one address can never hold every shared slot. That global gate and the
 * {@code vision.identification} bulkhead stay in the core, where they bound every caller;
 * this filter only keeps one address from spending them all. What "client" means is ADR-027
 * D4: the container-rewritten remote address, never a header the app reads.
 *
 * <p>The decision is observed in {@link Observer.MonitoringMode#ALWAYS} as
 * {@code withinClientRate} -- the violations the flood signal, the valid rows the identify
 * baseline. Nothing is logged: the client key is personal data and is never a metric tag
 * (unbounded cardinality), so the observer's count is the whole record. Constructed by
 * {@code SecurityConfiguration} directly (not a bean) so it sits in the security chain exactly
 * once; it declares {@code @Resilient} and reaches the facade in its constructor, satisfying
 * both resilience gates.
 */
public @Resilient(name = IdentifyRateLimitFilter.STRATEGY)
final class IdentifyRateLimitFilter extends OncePerRequestFilter {
    static final String STRATEGY = "vision.identification.client";

    private static final RequestMatcher IDENTIFY = PathPatternRequestMatcher.withDefaults()
            .matcher(HttpMethod.POST, "/{domain:[a-z]+}/identify");

    private final Observer observer = Observer.forClass(getClass(), Observer.MonitoringMode.ALWAYS);

    private final RateLimiter perClient;

    public IdentifyRateLimitFilter(Resilience resilience) {
        this.perClient = resilience.rateLimiter(STRATEGY);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        RequestMatcher.MatchResult identify = IDENTIFY.matcher(request);
        if (!identify.isMatch()) {
            chain.doFilter(request, response);
            return;
        }
        MethodObserver doFilterInternal = observer.forMethod("doFilterInternal");
        boolean clientPermitted = acquire(perClient.forKey(ClientKey.of(request)));
        doFilterInternal.arguments(i -> i.isTrue(clientPermitted, "withinClientRate"))
                .observe(Level.WARN);
        if (!clientPermitted) {
            response.setStatus(HttpServletResponse.SC_SEE_OTHER);
            response.setHeader("Location", request.getContextPath()
                    + "/" + identify.getVariables().get("domain") + "/identify?limit=RATE");
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean acquire(RateLimiter limiter) {
        try {
            limiter.execute(() -> { });
            return true;
        } catch (RateLimitExceededException rejected) {
            return false;
        }
    }
}
