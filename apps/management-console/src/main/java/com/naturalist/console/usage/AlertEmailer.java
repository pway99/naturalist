package com.naturalist.console.usage;

import com.naturalist.usage.UsageAlert;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Best-effort, guarded email sender for {@link UsageAlert}s (task D1).
 *
 * <p>SMTP is optional infrastructure: most environments (dev, CI, and any
 * deployment without {@code spring.mail.*} configured) have no
 * {@link JavaMailSender} bean at all. {@link #send} resolves the sender via
 * {@link ObjectProvider#getIfAvailable()} and no-ops when it is absent,
 * rather than requiring every environment to configure SMTP just to boot.
 *
 * <p>Delivery failures (a misconfigured host, a bounced address, a transient
 * network error) are swallowed rather than propagated or logged — this
 * codebase has no logging infrastructure, and the persisted {@link UsageAlert}
 * row (see {@code UsageQuery#activeAlerts()}) is the durable record of the
 * alert regardless of whether the email made it out. Task D2's scheduled job
 * is the only caller; it must never fail a batch because one address bounced.
 */
@Component
class AlertEmailer {

    private final ObjectProvider<JavaMailSender> mailProvider;
    private final UsageProperties properties;

    AlertEmailer(ObjectProvider<JavaMailSender> mailProvider, UsageProperties properties) {
        this.mailProvider = mailProvider;
        this.properties = properties;
    }

    void send(UsageAlert alert) {
        JavaMailSender sender = mailProvider.getIfAvailable();
        if (sender == null) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(properties.alertEmail());
            message.setSubject("[naturalist] " + alert.kind() + " " + alert.scope());
            message.setText(alert.message());
            sender.send(message);
        } catch (Exception ignored) {
            // Best-effort: the persisted alert row is the record; see class javadoc.
        }
    }
}
