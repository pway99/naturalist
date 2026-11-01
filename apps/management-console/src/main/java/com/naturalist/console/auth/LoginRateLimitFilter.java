package com.naturalist.console.auth;

import com.naturalist.observability.Level;
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

/**
 * Global ceiling on authentication attempts, ahead of Spring's login filter. {@code POST /login}
 * takes one {@code auth.login} permit (see {@code ResilienceConfiguration}); past the ceiling the
 * request is answered {@code 429 Too Many Requests} with {@code Retry-After} and never reaches
 * the password check — so a credential-stuffing run cannot burn a BCrypt round per guess
 * without bound.
 *
 * <p>The ceiling is global, not per client: the kernel {@code RateLimiter} is name-scoped. That
 * caps system-wide CPU and bounds the {@code badCredentials} metric's worst case, but an
 * attacker who exhausts the budget also locks honest users out for the window — the limit is
 * sized so that never happens at honest rates, and the targeted response to one abuser is
 * {@code AccountCommand.suspend}. A per-client limiter is the planned follow-up.
 *
 * <p>Every permit decision is observed as {@code withinRate} in
 * {@link Observer.MonitoringMode#ALWAYS}: the violations are the flood signal, the valid rows
 * the attempt baseline. Constructed by {@code SecurityConfiguration} directly (not a bean) so it
 * sits in the security chain exactly once; it declares {@code @Resilient} and reaches the facade
 * in its constructor, satisfying both resilience gates.
 */
public @Resilient(name = LoginRateLimitFilter.STRATEGY)
final class LoginRateLimitFilter extends OncePerRequestFilter {
    static final String STRATEGY = "auth.login";

    private final Observer observer = Observer.forClass(getClass(), Observer.MonitoringMode.ALWAYS);

    private final RateLimiter limiter;

    public LoginRateLimitFilter(Resilience resilience) {
        this.limiter = resilience.rateLimiter(STRATEGY);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (!("POST".equals(request.getMethod()) && "/login".equals(request.getServletPath()))) {
            chain.doFilter(request, response);
            return;
        }
        boolean permitted = acquire();
        observer.forMethod("doFilterInternal")
                .arguments(i -> i.isTrue(permitted, "withinRate"))
                .observe(Level.WARN);
        if (!permitted) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Too many login attempts right now — please try again in a minute.");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean acquire() {
        try {
            limiter.execute(() -> { });
            return true;
        } catch (RateLimitExceededException rejected) {
            return false;
        }
    }
}
