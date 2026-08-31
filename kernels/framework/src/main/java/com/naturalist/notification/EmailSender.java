package com.naturalist.notification;

/**
 * Outbound-email port: the single seam the whole system sends mail through, so the vendor
 * transport (SMTP via Spring's {@code MailSender}, a future HTTP provider) is an adapter
 * choice made once at the composition root — never a dependency of the code that decides
 * an email should be sent. Mirrors the {@link com.naturalist.resilience.Resilience} facade:
 * a pure port here, the vendor adapter in {@code adapters/}.
 *
 * <p>Implementations are <strong>best-effort and never throw</strong>. A caller that has
 * decided to send an email is not the place to handle a bounced address or a transport
 * outage: the durable record of intent (a persisted verification token, a persisted alert
 * row) is the caller's responsibility, and it exists whether or not the message goes out.
 * The default {@link LoggingEmailSender} records the message to the log — which is what lets
 * the auth flows run end-to-end with no SMTP server — and the SMTP adapter sends it and
 * swallows transport failures.
 */
public interface EmailSender {

    /** Sends {@code message}. Never throws — delivery is best-effort (see the type javadoc). */
    void send(EmailMessage message);
}
