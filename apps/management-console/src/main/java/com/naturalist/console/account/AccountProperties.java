package com.naturalist.console.account;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Property-bound deployment policy under {@code naturalist.access.*} for how vision access is
 * granted. {@code grantPolicy} is one of {@code self-serve} (verifying email immediately grants
 * VISION) or {@code request} (verifying email leaves the account BROWSE_ONLY until an admin
 * grants VISION) — see {@link com.naturalist.account.AccountPolicy}. Defaults to
 * {@code self-serve} when unset.
 */
@ConfigurationProperties("naturalist.access")
public record AccountProperties(String grantPolicy) {

    public AccountProperties {
        if (grantPolicy == null || grantPolicy.isBlank()) {
            grantPolicy = "self-serve";
        }
    }
}
