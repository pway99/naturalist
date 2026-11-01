package com.naturalist.console.usage;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.notification.EmailMessage;
import com.naturalist.notification.EmailSender;
import com.naturalist.usage.AlertKind;
import com.naturalist.usage.AlertScope;
import com.naturalist.usage.UsageAlert;
import com.naturalist.usage.UsageAlertId;
import com.naturalist.usage.UsageCommand;
import com.naturalist.usage.UsageCounterName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test (no Spring context) for {@link AlertDispatchJob} — proves the "email once"
 * guarantee: {@link UsageCommand#claimUnsentAlerts()} returns the pending alert on the first
 * call and an empty list on every call after, so {@link AlertEmailer#send} must fire exactly
 * once across two {@code dispatch()} invocations regardless of how often the scheduler ticks.
 */
class AlertDispatchJobTest {

    private static final UsageAlert ALERT = new UsageAlert(
            UsageAlertId.create(),
            UsageCounterName.of("identification"),
            AlertScope.DAILY,
            AlertKind.WARNING,
            "daily-2026-08-25",
            "daily identification budget warning: 40/50 used",
            Instant.parse("2026-08-25T15:00:00Z"),
            false,
            false);

    @Test
    void emailsClaimedAlertExactlyOnceAcrossRepeatedDispatches() {
        ClaimOnceCommand command = new ClaimOnceCommand(List.of(ALERT));
        CountingEmailSender sender = new CountingEmailSender();
        AlertEmailer emailer = new AlertEmailer(sender, new UsageProperties(80, "alerts@example.com"));
        AlertDispatchJob job = new AlertDispatchJob(command, emailer);

        job.dispatch();
        job.dispatch();

        assertThat(sender.sendCount.get()).isEqualTo(1);
    }

    /** Returns the given alerts once, then an empty list forever after — mirrors the atomic claim-and-mark contract of {@code UsageCommand#claimUnsentAlerts()}. */
    private static final class ClaimOnceCommand implements UsageCommand {

        private List<UsageAlert> nextClaim;

        ClaimOnceCommand(List<UsageAlert> firstClaim) {
            this.nextClaim = firstClaim;
        }

        @Override
        public void reserve(NaturalistName naturalist) {
            throw new UnsupportedOperationException("not used by AlertDispatchJob");
        }

        @Override
        public void acknowledge(UsageAlertId id) {
            throw new UnsupportedOperationException("not used by AlertDispatchJob");
        }

        @Override
        public List<UsageAlert> claimUnsentAlerts() {
            List<UsageAlert> claimed = nextClaim;
            nextClaim = List.of();
            return claimed;
        }
    }

    /** Counts {@link EmailSender#send} invocations. */
    private static final class CountingEmailSender implements EmailSender {

        private final AtomicInteger sendCount = new AtomicInteger();

        @Override
        public void send(EmailMessage message) {
            sendCount.incrementAndGet();
        }
    }
}
