package com.naturalist.console.challenge;

import com.naturalist.challenge.ChallengeVerifier;
import com.naturalist.challenge.LogOnlyChallengeVerifier;
import com.naturalist.challenge.turnstile.TurnstileChallengeVerifier;
import com.naturalist.resilience.Resilience;
import com.naturalist.secrets.EnvironmentSecretProvider;
import com.naturalist.secrets.SecretProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The verifier choice fails closed (ADR-027 D6). No Spring context: the configuration method is
 * called directly, so the question is only which verifier comes back, or whether boot is refused,
 * and what is observed.
 */
class ChallengeConfigurationTest {

    private static final SecretProvider NO_SECRET = new EnvironmentSecretProvider(Map.of());
    private static final SecretProvider WITH_SECRET =
            new EnvironmentSecretProvider(Map.of("CHALLENGE_CREDENTIAL", "not-a-real-secret"));

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final ChallengeConfiguration configuration = new ChallengeConfiguration();

    @BeforeEach
    void setUp() {
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void neitherSecretNorFlag_refusesToBoot_namingBothRemedies_andIsObserved() {
        assertThatThrownBy(() -> configuration.challengeVerifier(NO_SECRET, properties("", false), Resilience.noOp()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("challenge-credential")
                .hasMessageContaining("naturalist.challenge.log-only=true");

        assertThat(violations("secretConfigured")).isEqualTo(1);
        assertThat(violations("turnstile")).isEqualTo(1);
    }

    @Test
    void flagOnly_wiresTheLogOnlyVerifier_andKeepsTheTurnstileViolationVisible() {
        ChallengeVerifier verifier = configuration.challengeVerifier(NO_SECRET, properties("", true), Resilience.noOp());

        assertThat(verifier).isInstanceOf(LogOnlyChallengeVerifier.class);
        assertThat(violations("secretConfigured")).isZero();
        assertThat(violations("turnstile")).isEqualTo(1);
    }

    @Test
    void secretPresent_wiresTurnstile_regardlessOfTheFlag() {
        ChallengeVerifier verifier =
                configuration.challengeVerifier(WITH_SECRET, properties("a-site-key", true), Resilience.noOp());

        assertThat(verifier).isInstanceOf(TurnstileChallengeVerifier.class);
        assertThat(violations("secretConfigured")).isZero();
        assertThat(violations("turnstile")).isZero();
        assertThat(violations("siteKeyConfigured")).isZero();
    }

    /** A live verifier with no widget would refuse every sign-up: every submission arrives tokenless. */
    @Test
    void secretWithoutSiteKey_refusesToBoot_andIsObserved() {
        assertThatThrownBy(() -> configuration.challengeVerifier(WITH_SECRET, properties(" ", false), Resilience.noOp()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("naturalist.challenge.site-key");

        assertThat(violations("siteKeyConfigured")).isEqualTo(1);
    }

    private static ChallengeProperties properties(String siteKey, boolean logOnly) {
        return new ChallengeProperties(siteKey, "backyardnaturalist.app", logOnly);
    }

    private double violations(String decision) {
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                .tag("name", "ChallengeConfiguration.challengeVerifier." + decision).counter();
        return counter == null ? 0 : counter.count();
    }
}
