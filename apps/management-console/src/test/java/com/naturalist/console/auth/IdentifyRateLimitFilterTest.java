package com.naturalist.console.auth;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.Naturalist;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.ResilienceConfig.RateLimiterConfig;
import com.naturalist.resilience.resilience4j.Resilience4jResilience;
import com.naturalist.usage.UsageCounterName;
import com.naturalist.usage.UsageEvent;
import com.naturalist.usage.UsageEventTestEntitySource;
import com.naturalist.usage.UsagesTestContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link IdentifyRateLimitFilter} over the real Resilience4j adapter -- no Spring context. Two
 * permits a minute per client address on {@code POST /<domain>/identify}: is the third
 * redirected to the identify page's {@code RATE} notice before it reaches the chain, does each address keep its own budget, do the organism domains
 * share one budget per address, are other requests untouched, and is each refusal observed --
 * and recorded against the signed-in naturalist, if there is one. The refusal is recorded by the real
 * usage {@code IdentificationBudget} from {@link UsagesTestContext}, and read back from the usage event log.
 */
class IdentifyRateLimitFilterTest {

    private static final String WITHIN_CLIENT_RATE = "IdentifyRateLimitFilter.doFilterInternal.withinClientRate";

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC);

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private Optional<NaturalistName> signedIn = Optional.empty();
    private IdentifyRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new RateLimiterConfig(IdentifyRateLimitFilter.STRATEGY, 2, Duration.ofMinutes(1), Duration.ZERO)));
        CurrentNaturalist current = new CurrentNaturalist() {
            @Override public Optional<NaturalistName> name() { return signedIn; }
            @Override public Optional<Naturalist> naturalist() { return Optional.empty(); }
        };
        filter = new IdentifyRateLimitFilter(resilience, current,
                UsagesTestContext.create(nte, 80, CLOCK).identificationBudget());
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void thirdIdentifyFromOneAddress_isRedirectedToTheRateNotice_andNeverReachesTheChain() throws Exception {
        assertThat(identify("10.0.0.1", "/insects/identify").getStatus()).isEqualTo(200);
        assertThat(identify("10.0.0.1", "/insects/identify").getStatus()).isEqualTo(200);

        var chain = new MockFilterChain();
        var refused = send("POST", "10.0.0.1", "/insects/identify", chain);

        assertThat(refused.getStatus()).isEqualTo(303);
        assertThat(refused.getHeader("Location")).isEqualTo("/insects/identify?limit=RATE");
        assertThat(chain.getRequest()).as("the refused request never reaches the chain").isNull();
        assertThat(violations(WITHIN_CLIENT_RATE)).isEqualTo(1);
    }

    @Test
    void anotherAddress_hasItsOwnBudget() throws Exception {
        identify("10.0.0.1", "/insects/identify");
        identify("10.0.0.1", "/insects/identify");
        identify("10.0.0.1", "/insects/identify");

        assertThat(identify("10.0.0.2", "/insects/identify").getStatus()).isEqualTo(200);
    }

    @Test
    void plantsIdentify_sharesTheSameClientBudget() throws Exception {
        identify("10.0.0.1", "/insects/identify");
        identify("10.0.0.1", "/plants/identify");

        var refused = identify("10.0.0.1", "/insects/identify");
        assertThat(refused.getStatus()).isEqualTo(303);
        assertThat(refused.getHeader("Location")).isEqualTo("/insects/identify?limit=RATE");
    }

    @Test
    void otherRequests_areNotCounted() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(send("GET", "10.0.0.1", "/insects/identify", new MockFilterChain()).getStatus()).isEqualTo(200);
            assertThat(send("POST", "10.0.0.1", "/insects/battus-philenor/images", new MockFilterChain()).getStatus())
                    .isEqualTo(200);
        }

        assertThat(identify("10.0.0.1", "/insects/identify").getStatus()).isEqualTo(200);
        assertThat(violations(WITHIN_CLIENT_RATE)).isZero();
    }

    /**
     * A per-client refusal of a signed-in naturalist is recorded against that naturalist -- the
     * name and nothing else, never the address -- for client relations; a permitted request
     * records nothing.
     */
    @Test
    void refusal_isRecordedAgainstTheSignedInNaturalist() throws Exception {
        var pat = NaturalistName.of("pat");
        signedIn = Optional.of(pat);
        identify("10.0.0.1", "/insects/identify");
        identify("10.0.0.1", "/insects/identify");
        assertThat(refusals()).as("permitted requests record no refusal").isEmpty();

        identify("10.0.0.1", "/insects/identify");

        assertThat(refusals()).containsExactly(pat);
    }

    @Test
    void refusal_withNoSignedInNaturalist_recordsNothing() throws Exception {
        identify("10.0.0.1", "/insects/identify");
        identify("10.0.0.1", "/insects/identify");

        assertThat(identify("10.0.0.1", "/insects/identify").getStatus()).isEqualTo(303);
        assertThat(refusals()).isEmpty();
    }

    private MockHttpServletResponse identify(String client, String path) throws Exception {
        return send("POST", client, path, new MockFilterChain());
    }

    private MockHttpServletResponse send(String method, String client, String path, MockFilterChain chain)
            throws Exception {
        var request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(client);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    /** The naturalists named by the usage event log's refusal events, in the order they were recorded. */
    private List<NaturalistName> refusals() {
        return nte.getNamed(UsageEventTestEntitySource.class).entityStream()
                .filter(e -> e.counterName().equals(UsageCounterName.of("identification-refused")))
                .map(UsageEvent::naturalist)
                .toList();
    }

    private double violations(String name) {
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                .tag("name", name).counter();
        return counter == null ? 0 : counter.count();
    }
}
