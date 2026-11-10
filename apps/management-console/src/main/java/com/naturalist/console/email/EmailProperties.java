package com.naturalist.console.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.apache.commons.lang3.StringUtils;

/**
 * Property-bound email settings under {@code naturalist.email.*}.
 *
 * <p>{@code from} is the envelope sender used on outbound mail (verification / reset links,
 * usage alerts). Defaults to a {@code naturalist.local} no-reply address when unset — good
 * enough for the log-only dev sender, overridden per deployment once a real sending domain is
 * configured.
 *
 * <p>{@code publicUrl} is the <strong>only</strong> origin an emailed link may be built from
 * ({@code https://naturalist.example}, no trailing slash). It is deliberately never derived
 * from the incoming request: a link built from the request's {@code Host} header (or an
 * unconfigured {@code X-Forwarded-Host}) lets an attacker who registers or resets with a
 * forged host put <em>their</em> origin in the victim's email, and the single-use token is
 * surrendered when the victim clicks it. Defaults to {@code http://localhost:8080} so a
 * deployment that forgets to set it sends links that fail safely (they go nowhere), never
 * links that go somewhere else.
 *
 * <p>{@code logOnly} is the explicit opt-in to the kernel's {@code LoggingEmailSender} when
 * no {@code spring.mail.*} transport is configured. It defaults to {@code false} and the
 * composition root refuses to boot without a transport unless it is {@code true}: the
 * log-only sender writes every verification and reset token to the log beside the recipient's
 * address, which in a deployment is an account-takeover primitive for anyone with log access.
 * Dev and CI set it; a deployment never should.
 */
@ConfigurationProperties("naturalist.email")
public record EmailProperties(String from, String publicUrl, boolean logOnly) {

    public EmailProperties {
        if (StringUtils.isBlank(from)) {
            from = "no-reply@naturalist.local";
        }
        if (StringUtils.isBlank(publicUrl)) {
            publicUrl = "http://localhost:8080";
        }
        publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }
}
