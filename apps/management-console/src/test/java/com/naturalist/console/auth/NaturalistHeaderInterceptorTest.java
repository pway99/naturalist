package com.naturalist.console.auth;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.usage.UsageAlert;
import com.naturalist.usage.UsageAlertId;
import com.naturalist.usage.UsageQuery;
import com.naturalist.usage.UsageSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalistHeaderInterceptorTest {

    /** No unacknowledged alerts — every existing assertion in this class predates the banner. */
    private static final UsageQuery NO_ALERTS = new UsageQuery() {
        @Override
        public UsageSnapshot snapshot() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UsageAlert> activeAlerts() {
            return List.of();
        }

        @Override
        public UsageQuery.ReserveState reserveState(
                com.naturalist.usage.UsageCounterName counter, NaturalistName naturalist, java.time.Instant now) {
            throw new UnsupportedOperationException();
        }
    };

    private final NaturalistHeaderInterceptor interceptor = new NaturalistHeaderInterceptor(NO_ALERTS);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publishesNaturalistSlug_forNaturalistPrincipal() {
        var principal = new NaturalistPrincipal(
                NaturalistName.of("patrick-way"), "Patrick", "gerald.durrell@oakvista.example",
                "{bcrypt}x", false, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities()));
        var request = new MockHttpServletRequest();

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(request.getAttribute(NaturalistHeaderInterceptor.CURRENT_NATURALIST_NAME))
                .isEqualTo("patrick-way");
    }

    @Test
    void publishesNull_forAnonymous() {
        var request = new MockHttpServletRequest();
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        assertThat(request.getAttribute(NaturalistHeaderInterceptor.CURRENT_NATURALIST_NAME)).isNull();
    }

    @Test
    void publishesInsectSectionAndLensAndUri() {
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/insects/species");
        request.setQueryString("page=2");
        var session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("insects.collectionLens", true);
        request.setSession(session);

        interceptor.preHandle(request, new org.springframework.mock.web.MockHttpServletResponse(), new Object());

        org.assertj.core.api.Assertions.assertThat(request.getAttribute("insectSection")).isEqualTo(true);
        org.assertj.core.api.Assertions.assertThat(request.getAttribute("collectionLens")).isEqualTo(true);
        org.assertj.core.api.Assertions.assertThat(request.getAttribute("requestUri")).isEqualTo("/insects/species?page=2");
    }

    @Test
    void insectSectionFalse_offInsectsPath() {
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/chemistry");
        interceptor.preHandle(request, new org.springframework.mock.web.MockHttpServletResponse(), new Object());
        org.assertj.core.api.Assertions.assertThat(request.getAttribute("insectSection")).isEqualTo(false);
        org.assertj.core.api.Assertions.assertThat(request.getAttribute("collectionLens")).isEqualTo(false);
    }

    @Test
    void usageAlertsPending_false_whenNoActiveAlerts() {
        setAdminAuthentication();
        var request = new MockHttpServletRequest();
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        assertThat(request.getAttribute("usageAlertsPending")).isEqualTo(false);
    }

    @Test
    void usageAlertsPending_true_whenAdminAndAnActiveAlertExists() {
        setAdminAuthentication();
        var request = new MockHttpServletRequest();

        new NaturalistHeaderInterceptor(oneAlert())
                .preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(request.getAttribute("usageAlertsPending")).isEqualTo(true);
    }

    /**
     * The banner must not leak operational alert state — or even call {@link
     * UsageQuery#activeAlerts()} — for a visitor who isn't ROLE_ADMIN. Uses
     * a monitor that fails the test if {@code activeAlerts()} is invoked at
     * all, proving the interceptor short-circuits on the role check first.
     */
    @Test
    void usageAlertsPending_false_forAnonymous_evenWithAlertsPending() {
        var request = new MockHttpServletRequest();

        new NaturalistHeaderInterceptor(explodesIfActiveAlertsCalled())
                .preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(request.getAttribute("usageAlertsPending")).isEqualTo(false);
    }

    @Test
    void usageAlertsPending_false_forNaturalistRole_evenWithAlertsPending() {
        var principal = new NaturalistPrincipal(
                NaturalistName.of("patrick-way"), "Patrick", "gerald.durrell@oakvista.example",
                "{bcrypt}x", false, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities()));
        var request = new MockHttpServletRequest();

        new NaturalistHeaderInterceptor(explodesIfActiveAlertsCalled())
                .preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(request.getAttribute("usageAlertsPending")).isEqualTo(false);
    }

    private static void setAdminAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "naturalist", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private static UsageQuery oneAlert() {
        return new UsageQuery() {
            @Override
            public UsageSnapshot snapshot() {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<UsageAlert> activeAlerts() {
                return List.of(new UsageAlert(
                        UsageAlertId.create(),
                        com.naturalist.usage.UsageCounterName.of("identification"),
                        com.naturalist.usage.AlertScope.MONTHLY,
                        com.naturalist.usage.AlertKind.WARNING,
                        "monthly-2026-08",
                        "monthly identification budget WARNING: 1/2 used",
                        java.time.Instant.now(),
                        false,
                        false));
            }

            @Override
            public UsageQuery.ReserveState reserveState(
                    com.naturalist.usage.UsageCounterName counter, NaturalistName naturalist, java.time.Instant now) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static UsageQuery explodesIfActiveAlertsCalled() {
        return new UsageQuery() {
            @Override
            public UsageSnapshot snapshot() {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<UsageAlert> activeAlerts() {
                throw new AssertionError(
                        "activeAlerts() must not be called for a non-ADMIN request — the interceptor "
                                + "should short-circuit on the role check first");
            }

            @Override
            public UsageQuery.ReserveState reserveState(
                    com.naturalist.usage.UsageCounterName counter, NaturalistName naturalist, java.time.Instant now) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
