package com.naturalist.notification;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Default {@link EmailSender}: logs the message instead of transmitting it. This is what
 * makes the auth flows work end-to-end with <strong>no SMTP server</strong> — in dev and
 * CI the verification / reset link is printed to the log, and a developer completes the
 * flow by reading it there. The composition root selects the SMTP adapter over this only
 * when a mail transport is configured.
 *
 * <p>Uses the JDK {@link System.Logger} rather than SLF4J so the kernel keeps its
 * dependency budget (Jackson / Commons / Micrometer / JSpecify only); the app's logging
 * backend picks it up through the {@code System.Logger} bridge.
 */
public final class LoggingEmailSender implements EmailSender {

    private static final Logger LOG = System.getLogger(LoggingEmailSender.class.getName());

    @Override
    public void send(EmailMessage message) {
        LOG.log(Level.INFO,
                "[email:log-only] no mail transport configured — message not sent."
                        + "\n  to: {0}\n  subject: {1}\n  body:\n{2}",
                message.to(), message.subject(), message.body());
    }
}
