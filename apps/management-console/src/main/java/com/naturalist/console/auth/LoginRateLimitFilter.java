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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.lang.System.Logger;

/**
 * Two ceilings on authentication attempts, ahead of Spring's login filter. {@code POST /login}
 * first takes one {@code auth.login.client} permit for the request's {@link ClientKey} (ten a
 * minute per client), then one {@code auth.login} permit system-wide (300 a minute); past
 * either the request is answered {@code 429 Too Many Requests} with {@code Retry-After} and
 * never reaches the password check — so one abuser is cut off on their own, and a
 * credential-stuffing run that rotates addresses still cannot burn a BCrypt round per guess
 * without bound.
 *
 * <p>Per-client first, deliberately: a single flooding client is refused by its own budget and
 * never consumes the system-wide one, so honest users are untouched by it. Only the
 * system-wide limiter sees a key-rotating attacker, which is why the pair exists
 * ({@code RateLimiter.forKey}'s contract; the adapter evicts idle keys).
 *
 * <p>Both decisions are observed in {@link Observer.MonitoringMode#ALWAYS} —
 * {@code withinClientRate} and {@code withinRate} — the violations the flood signal, the valid
 * rows the attempt baseline. The client key is never a metric tag (unbounded cardinality); a
 * client rejection logs it at {@code WARNING} instead. Constructed by
 * {@code SecurityConfiguration} directly (not a bean) so it sits in the security chain exactly
 * once; it declares {@code @Resilient} and reaches the facade in its constructor, satisfying
 * both resilience gates.
 */
public @Resilient(name = LoginRateLimitFilter.STRATEGY)
final class LoginRateLimitFilter extends OncePerRequestFilter {
    static final String STRATEGY = "auth.login";
    static final String CLIENT_STRATEGY = "auth.login.client";

    private static final Logger LOG = System.getLogger(LoginRateLimitFilter.class.getName());

    private final Observer observer = Observer.forClass(getClass(), Observer.MonitoringMode.ALWAYS);

    private final RateLimiter limiter;
    private final RateLimiter perClient;

    public LoginRateLimitFilter(Resilience resilience) {
        this.limiter = resilience.rateLimiter(STRATEGY);
        this.perClient = resilience.rateLimiter(CLIENT_STRATEGY);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (!("POST".equals(request.getMethod()) && "/login".equals(request.getServletPath()))) {
            chain.doFilter(request, response);
            return;
        }
        MethodObserver doFilterInternal = observer.forMethod("doFilterInternal");
        String client = ClientKey.of(request);
        boolean clientPermitted = acquire(perClient.forKey(client));
        // The system-wide permit is only spent once the client's own budget allowed the attempt.
        boolean permitted = !clientPermitted || acquire(limiter);
        doFilterInternal.arguments(i -> i
                        .isTrue(clientPermitted, "withinClientRate")
                        .isTrue(permitted, "withinRate"))
                .observe(Level.WARN);
        if (!clientPermitted) {
            LOG.log(Logger.Level.WARNING, "[auth.login.client] login attempts from {0} over the per-client ceiling", client);
        }
        if (!clientPermitted || !permitted) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Too many login attempts right now — please try again in a minute.");
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
