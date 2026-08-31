package com.naturalist.console.usage;

import com.naturalist.notification.EmailMessage;
import com.naturalist.notification.EmailSender;
import com.naturalist.usage.UsageAlert;
import org.springframework.stereotype.Component;

/**
 * Sends {@link UsageAlert}s through the shared {@link EmailSender} seam (task D1). Transport
 * presence/absence and delivery-failure handling now live in the port's implementations
 * (log-only when no transport is configured; swallow transport failures when it is), so this
 * collaborator is a thin translator from an alert to an {@link EmailMessage}.
 *
 * <p>The persisted {@link UsageAlert} row (see {@code UsageQuery#activeAlerts()}) remains the
 * durable record of the alert regardless of whether the email is delivered — task D2's
 * scheduled job, the only caller, must never fail a batch because one address bounced, and
 * the best-effort {@code EmailSender} contract guarantees {@link #send} cannot throw.
 */
@Component
class AlertEmailer {

    private final EmailSender emailSender;
    private final UsageProperties properties;

    AlertEmailer(EmailSender emailSender, UsageProperties properties) {
        this.emailSender = emailSender;
        this.properties = properties;
    }

    void send(UsageAlert alert) {
        emailSender.send(new EmailMessage(
                properties.alertEmail(),
                "[naturalist] " + alert.kind() + " " + alert.scope(),
                alert.message()));
    }
}
