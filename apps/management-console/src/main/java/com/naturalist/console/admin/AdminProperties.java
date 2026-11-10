package com.naturalist.console.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.apache.commons.lang3.StringUtils;

/**
 * Property-bound credentials for the {@code /admin/**} surface.
 *
 * <p>No defaults: the canonical constructor rejects null or blank
 * values, so a missing or empty {@code naturalist.admin.username} or
 * {@code naturalist.admin.password} fails Spring's
 * {@code @ConfigurationProperties} binding and the application refuses
 * to start. This is deliberate — the admin console is not allowed to
 * fall back to a hard-coded default credential pair.
 */
@ConfigurationProperties("naturalist.admin")
public record AdminProperties(String username, String password) {

    public AdminProperties {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException(
                    "naturalist.admin.username must be set and non-blank");
        }
        if (StringUtils.isBlank(password)) {
            throw new IllegalArgumentException(
                    "naturalist.admin.password must be set and non-blank");
        }
    }
}
