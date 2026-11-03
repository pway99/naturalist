package com.naturalist.console.challenge;

import com.naturalist.challenge.ChallengeVerifier;
import com.naturalist.challenge.LogOnlyChallengeVerifier;
import com.naturalist.challenge.turnstile.TurnstileChallengeVerifier;
import com.naturalist.challenge.turnstile.TurnstileConfig;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.Resilience;
import com.naturalist.secrets.SecretProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the {@link ChallengeVerifier} at the composition root, failing closed like
 * {@code EmailConfiguration}: the Turnstile adapter whenever the {@code challenge-credential}
 * secret resolves, otherwise the log-only verifier, but <strong>only</strong> when
 * {@code naturalist.challenge.log-only=true} opts into it. With neither the application refuses to
 * start, because a deployment that merely forgot the secret must not run with the challenge off.
 *
 * <p>Every decision is observed under {@code ChallengeConfiguration.challengeVerifier}: a log-only
 * instance carries a standing {@code turnstile} violation so it shows on the dashboard.
 */
@Configuration
@EnableConfigurationProperties(ChallengeProperties.class)
class ChallengeConfiguration {

    private final Observer observer = Observer.forClass(ChallengeConfiguration.class, Observer.MonitoringMode.ALWAYS);

    @Bean
    ChallengeVerifier challengeVerifier(SecretProvider secrets, ChallengeProperties properties, Resilience resilience) {
        boolean secret = secrets.lookup(TurnstileChallengeVerifier.CREDENTIAL).isPresent();
        boolean logOnly = properties.logOnly();
        boolean siteKey = properties.siteKey() != null && !properties.siteKey().isBlank();
        observer.forMethod("challengeVerifier").state((Constraints c) -> c
                .isTrue(secret || logOnly, "secretConfigured")
                .isTrue(secret, "turnstile")
                .isTrue(!secret || siteKey, "siteKeyConfigured")).observe(Level.WARN);
        if (secret) {
            if (!siteKey) {
                // A live verifier with no widget refuses every sign-up: each one arrives tokenless.
                throw new IllegalStateException(
                        "the challenge-credential secret is set but naturalist.challenge.site-key is not");
            }
            return new TurnstileChallengeVerifier(
                    TurnstileConfig.of(properties.expectedHostname()), secrets, resilience);
        }
        if (!logOnly) {
            throw new IllegalStateException(
                    "no challenge verifier configured: supply the challenge-credential secret, or set"
                            + " naturalist.challenge.log-only=true to opt into the log-only verifier (dev / CI only)");
        }
        return new LogOnlyChallengeVerifier();
    }
}
