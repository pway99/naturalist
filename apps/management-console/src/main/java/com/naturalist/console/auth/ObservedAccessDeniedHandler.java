package com.naturalist.console.auth;

import com.naturalist.observability.Handling;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Counts every 403 the security chain answers a signed-in caller with, then answers it exactly as
 * Spring's default handler does. Without it an authorization refusal left no count at all (rule B1):
 * a thousand unverified write attempts in a night would look like silence.
 *
 * <p>The refusal is observed under the rule that refused it, as a fact about the caller: {@code
 * csrfTokenValid} (a state-changing request without a valid CSRF token), {@code hasVision} (identify
 * without the VISION entitlement), {@code emailVerified} (a member write before the email is verified,
 * launch 1.5), {@code hasAdminRole} (the admin console). {@code routeRuleKnown} is a 403 from a rule
 * this handler does not know: add the rule here when it fires. The route patterns are declared here and
 * read by {@code SecurityConfiguration}, so the rule and its name cannot drift apart.
 *
 * <p>An anonymous caller never reaches this handler: the chain sends it to the login page instead.
 * Neither the path nor the caller is tagged (unbounded cardinality, and the caller is PII).
 */
public final class ObservedAccessDeniedHandler implements AccessDeniedHandler {

    /** The identify endpoints, which need {@code VISION}. */
    public static final String[] IDENTIFY = {"/insects/identify", "/plants/identify"};

    /** The member writes (sighting, photo, notes), which need {@link NaturalistPrincipal#VERIFIED}. */
    public static final String[] MEMBER_WRITES = {
            "/insects/*/observe", "/insects/*/images", "/insects/*/notes",
            "/plants/*/observe", "/plants/*/images", "/plants/*/notes"};

    /** The admin console, which needs {@code ROLE_ADMIN}. */
    public static final String ADMIN = "/admin/**";

    private static final Observer observer =
            Observer.forClass(ObservedAccessDeniedHandler.class, Observer.MonitoringMode.ALWAYS);

    private final RequestMatcher identify = posts(IDENTIFY);
    private final RequestMatcher memberWrites = posts(MEMBER_WRITES);
    private final RequestMatcher admin = PathPatternRequestMatcher.withDefaults().matcher(ADMIN);
    private final AccessDeniedHandler delegate = new AccessDeniedHandlerImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException denied)
            throws IOException, ServletException {
        MethodObserver methodObserver = observer.forMethod("handle");
        Authentication caller = SecurityContextHolder.getContext().getAuthentication();
        boolean csrfTokenValid = !(denied instanceof CsrfException);
        if (!csrfTokenValid) {
            methodObserver.state(i -> i.isTrue(csrfTokenValid, "csrfTokenValid")).observe(Level.WARN);
        } else if (identify.matches(request)) {
            methodObserver.state(i -> i.isTrue(holds(caller, "VISION"), "hasVision")).observe(Level.WARN);
        } else if (memberWrites.matches(request)) {
            methodObserver.state(i -> i.isTrue(holds(caller, NaturalistPrincipal.VERIFIED), "emailVerified"))
                    .observe(Level.WARN);
        } else if (admin.matches(request)) {
            methodObserver.state(i -> i.isTrue(holds(caller, "ROLE_ADMIN"), "hasAdminRole")).observe(Level.WARN);
        } else {
            boolean routeRuleKnown = false;   // no rule above matched this refusal
            methodObserver.state(i -> i.isTrue(routeRuleKnown, "routeRuleKnown")).observe(Level.WARN);
        }
        try {
            delegate.handle(request, response, denied);
        } catch (IOException | ServletException failed) {
            methodObserver.noticeError("forbiddenAnswered", failed, Handling.RETHROWN);
            throw failed;
        }
    }

    private static boolean holds(Authentication caller, String authority) {
        return caller != null && caller.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private static RequestMatcher posts(String[] patterns) {
        PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher.withDefaults();
        List<RequestMatcher> matchers = Arrays.stream(patterns)
                .map(pattern -> (RequestMatcher) builder.matcher(HttpMethod.POST, pattern))
                .toList();
        return new OrRequestMatcher(matchers);
    }
}
