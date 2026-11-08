package com.naturalist.console.usage;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.notification.EmailMessage;
import com.naturalist.notification.EmailSender;
import com.naturalist.usage.AlertKind;
import com.naturalist.usage.AlertScope;
import com.naturalist.usage.UsageAlert;
import com.naturalist.usage.UsageAlertId;
import com.naturalist.usage.UsageAlertTestEntitySource;
import com.naturalist.usage.UsageCommand;
import com.naturalist.usage.UsageCounterName;
import com.naturalist.usage.UsagesTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test (no Spring context) for {@link AlertDispatchJob} over the real {@link UsageCommand} from
 * {@link UsagesTestContext} — proves the "email once" guarantee through real state: one unsent alert in the
 * usage alert source is claimed (and marked emailed) by the first {@code dispatch()}, so the second finds
 * nothing and {@link AlertEmailer#send} fires exactly once however often the scheduler ticks.
 */
class AlertDispatchJobTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-25T15:00:00Z"), ZoneOffset.UTC);

    private static final UsageAlert ALERT = new UsageAlert(
            UsageAlertId.create(),
            UsageCounterName.of("identification"),
            AlertScope.DAILY,
            AlertKind.WARNING,
            "daily-2026-08-25",
            "daily identification budget warning: 40/50 used",
            CLOCK.instant(),
            false,
            false);

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void emailsClaimedAlertExactlyOnceAcrossRepeatedDispatches() {
        nte.getNamed(UsageAlertTestEntitySource.class).insert(ALERT);
        UsageCommand command = UsagesTestContext.create(nte, 80, CLOCK).usageCommand();
        CountingEmailSender sender = new CountingEmailSender();
        AlertEmailer emailer = new AlertEmailer(sender, new UsageProperties(80, "alerts@example.com"));
        AlertDispatchJob job = new AlertDispatchJob(command, emailer);

        job.dispatch();
        job.dispatch();

        assertThat(sender.sendCount.get()).isEqualTo(1);
        assertThat(nte.getNamed(UsageAlertTestEntitySource.class).getByName(ALERT.id()).orElseThrow().emailed())
                .as("the claim marks the alert emailed")
                .isTrue();
    }

    /** Counts {@link EmailSender#send} invocations. The mail sender is the outside world here. */
    private static final class CountingEmailSender implements EmailSender {

        private final AtomicInteger sendCount = new AtomicInteger();

        @Override
        public void send(EmailMessage message) {
            sendCount.incrementAndGet();
        }
    }
}
