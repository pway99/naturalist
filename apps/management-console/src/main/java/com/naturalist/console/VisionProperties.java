package com.naturalist.console;

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.apache.commons.lang3.StringUtils;

/**
 * {@code naturalist.vision.provider}: {@code none} or {@code anthropic} (ADR-028). It has no
 * default, like {@code AdminProperties}: a deployment that forgets it fails boot rather than
 * silently running the no-op, and an exported variable can never select a vendor by itself.
 */
@ConfigurationProperties("naturalist.vision")
public record VisionProperties(String provider) {
    public VisionProperties {
        if (StringUtils.isBlank(provider)) {
            throw new IllegalArgumentException("naturalist.vision.provider must be set: none or anthropic");
        }
        String normalized = provider.strip().toLowerCase(Locale.ROOT);
        if (!normalized.equals("none") && !normalized.equals("anthropic")) {
            throw new IllegalArgumentException(
                    "naturalist.vision.provider must be none or anthropic (was '" + provider + "')");
        }
        provider = normalized;
    }
}
