package com.naturalist.notification;

/**
 * Outbound-email port: the single seam the whole system sends mail through, so the vendor
 * transport (SMTP via Spring's {@code MailSender}, a future HTTP provider) is an adapter
 * choice made once at the composition root — never a dependency of the code that decides
 * an email should be sent. Mirrors the {@link com.naturalist.resilience.Resilience} facade:
 * a pure port here, the vendor adapter in {@code adapters/}.
 *
 * <p>The contract has two halves, and they fail differently:
 * <ul>
 *   <li><strong>An invalid message is a caller defect</strong> and throws
 *       {@link com.naturalist.exception.InvariantViolationException}. Every implementation
 *       observes its argument first ({@code observer().arguments("send", ...)}) exactly as a
 *       command or query does — a blank recipient or an empty body is never silently logged
 *       or handed to a transport.</li>
 *   <li><strong>Delivery is best-effort and never throws.</strong> A caller that has decided
 *       to send a valid email is not the place to handle a bounced address or a transport
 *       outage: the durable record of intent (a persisted verification token, a persisted
 *       alert row) is the caller's responsibility, and it exists whether or not the message
 *       goes out. The default {@link LoggingEmailSender} records the message to the log —
 *       which is what lets the auth flows run end-to-end with no SMTP server — and the SMTP
 *       adapter sends it and swallows transport failures.</li>
 * </ul>
 */
public interface EmailSender {

    /**
     * Sends {@code message}.
     *
     * @throws com.naturalist.exception.InvariantViolationException if {@code message} fails
     *         its {@link EmailMessage#invariants()}; delivery failures never propagate
     *         (see the type javadoc)
     */
    void send(EmailMessage message);
}
