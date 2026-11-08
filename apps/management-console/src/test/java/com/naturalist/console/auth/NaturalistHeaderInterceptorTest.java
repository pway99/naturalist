package com.naturalist.console.auth;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.usage.AlertKind;
import com.naturalist.usage.AlertScope;
import com.naturalist.usage.UsageAlert;
import com.naturalist.usage.UsageAlertId;
import com.naturalist.usage.UsageAlertTestEntitySource;
import com.naturalist.usage.UsageCounterName;
import com.naturalist.usage.UsagesTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link NaturalistHeaderInterceptor} over the real usage query from {@link UsagesTestContext}. The seeded
 * alert catalog is empty, so a test that needs a pending alert inserts one into the usage alert source.
 */
class NaturalistHeaderInterceptorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC);

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    private NaturalistHeaderInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new NaturalistHeaderInterceptor(UsagesTestContext.create(nte, 80, CLOCK).usageQuery());
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publishesNaturalistSlug_forNaturalistPrincipal() {
        setNaturalistAuthentication();
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
        var request = new MockHttpServletRequest("GET", "/insects/species");
        request.setQueryString("page=2");
        var session = new MockHttpSession();
        session.setAttribute("insects.collectionLens", true);
        request.setSession(session);

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(request.getAttribute("insectSection")).isEqualTo(true);
        assertThat(request.getAttribute("collectionLens")).isEqualTo(true);
        assertThat(request.getAttribute("requestUri")).isEqualTo("/insects/species?page=2");
    }

    @Test
    void insectSectionFalse_offInsectsPath() {
        var request = new MockHttpServletRequest("GET", "/chemistry");
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        assertThat(request.getAttribute("insectSection")).isEqualTo(false);
        assertThat(request.getAttribute("collectionLens")).isEqualTo(false);
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
        insertPendingAlert();
        setAdminAuthentication();
        var request = new MockHttpServletRequest();

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(request.getAttribute("usageAlertsPending")).isEqualTo(true);
        assertThat(alertReads()).as("the admin path is the one that reads them").isPositive();
    }

    /** The banner must not leak operational alert state to a visitor who isn't ROLE_ADMIN, with an alert pending. */
    @Test
    void usageAlertsPending_false_forAnonymous_evenWithAlertsPending() {
        insertPendingAlert();
        var request = new MockHttpServletRequest();

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(request.getAttribute("usageAlertsPending")).isEqualTo(false);
        assertThat(alertReads()).as("a visitor who isn't ROLE_ADMIN never consults the alerts").isZero();
    }

    @Test
    void usageAlertsPending_false_forNaturalistRole_evenWithAlertsPending() {
        insertPendingAlert();
        setNaturalistAuthentication();
        var request = new MockHttpServletRequest();

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(request.getAttribute("usageAlertsPending")).isEqualTo(false);
        assertThat(alertReads()).as("a naturalist who isn't ROLE_ADMIN never consults the alerts").isZero();
    }

    /** One unacknowledged, unemailed monthly warning: what the admin banner reports as pending. */
    private void insertPendingAlert() {
        nte.getNamed(UsageAlertTestEntitySource.class).insert(new UsageAlert(
                UsageAlertId.create(),
                UsageCounterName.of("identification"),
                AlertScope.MONTHLY,
                AlertKind.WARNING,
                "monthly-2026-08",
                "monthly identification budget WARNING: 1/2 used",
                CLOCK.instant(),
                false,
                false));
    }

    private static void setNaturalistAuthentication() {
        var principal = new NaturalistPrincipal(
                NaturalistName.of("patrick-way"), "Patrick", "gerald.durrell@oakvista.example",
                "{bcrypt}x", false, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities()));
    }

    private static void setAdminAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "naturalist", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    /** The reads of the usage alert source in this test; the role gate is proved by its staying zero. */
    private int alertReads() {
        return nte.getNamed(UsageAlertTestEntitySource.class).reads();
    }
}
