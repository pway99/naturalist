package com.naturalist.console.account;

import com.naturalist.observability.Handling;
import com.naturalist.observability.Observer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs {@link UnverifiedAccountPurgeTransaction} on a schedule, hourly by default, so a registration
 * left unverified is gone within about an hour of its 24-hour link expiring. The cron is
 * {@code naturalist.accounts.purge-cron}; {@code "-"} disables the job (the tests do, so no run ever
 * touches the standing test database). Scheduling itself is enabled by {@code UsageConfiguration}.
 *
 * <p>A failed run is noticed as {@code purged} and swallowed: the batch rolled back, and the next
 * run retries it, so nothing is lost and the scheduler thread stays healthy.
 */
@Component
class UnverifiedAccountPurgeJob {

    private static final Observer observer = Observer.forClass(UnverifiedAccountPurgeJob.class);
    private final UnverifiedAccountPurgeTransaction transaction;

    UnverifiedAccountPurgeJob(UnverifiedAccountPurgeTransaction transaction) {
        this.transaction = transaction;
    }

    @Scheduled(cron = "${naturalist.accounts.purge-cron:0 17 * * * *}")
    void purge() {
        try {
            transaction.purge();
        } catch (RuntimeException failed) {
            observer.noticeError("purge", "purged", failed, Handling.SWALLOWED);
        }
    }
}
