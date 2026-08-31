package com.naturalist.notification.spring;

import com.naturalist.notification.EmailMessage;
import com.naturalist.notification.EmailSender;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * {@link EmailSender} over Spring's {@link MailSender} (SMTP transport). Best-effort by
 * contract: a transport failure is logged and swallowed, never propagated — the caller has
 * already committed the durable record (a persisted token, an alert row), and a bounced
 * send must not fail its request.
 *
 * <p>Constructed at the app composition root from the {@code JavaMailSender} bean Spring
 * Boot autoconfigures when {@code spring.mail.*} is populated; when it is not, the app wires
 * the kernel's {@code LoggingEmailSender} instead. Enabling SMTP in production is therefore
 * a config-only change — no code path here changes.
 *
 * <p>Depends only on {@code MailSender} / {@link SimpleMailMessage} (plain-text send), not
 * the {@code javamail} subpackage, so the adapter compiles without a {@code jakarta.mail}
 * dependency; the concrete {@code JavaMailSender} the app injects is a {@code MailSender}.
 */
public final class SmtpEmailSender implements EmailSender {

    private static final Logger LOG = System.getLogger(SmtpEmailSender.class.getName());

    private final MailSender mailSender;
    private final String fromAddress;

    public SmtpEmailSender(MailSender mailSender, String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(EmailMessage message) {
        var mail = new SimpleMailMessage();
        mail.setFrom(fromAddress);
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.body());
        try {
            mailSender.send(mail);
        } catch (MailException transportFailure) {
            LOG.log(Level.WARNING,
                    "[email:smtp] delivery to {0} failed; swallowed (best-effort). subject: {1}",
                    message.to(), message.subject());
        }
    }
}
