package com.naturalist.console.challenge;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code naturalist.challenge.*}: the public half of the bot challenge (ADR-027 D6). The credential
 * is not here; it is the {@code challenge-credential} secret (ADR-028).
 *
 * <p>{@code siteKey} is public and goes in the page. {@code expectedHostname} is checked against
 * the vendor's answer; left blank, the check is skipped and a standing violation says so.
 * {@code logOnly} opts into a verifier that passes everything, for dev and CI only; a deployment
 * never sets it.
 */
@ConfigurationProperties("naturalist.challenge")
public record ChallengeProperties(@Nullable String siteKey, @Nullable String expectedHostname, boolean logOnly) {
}
