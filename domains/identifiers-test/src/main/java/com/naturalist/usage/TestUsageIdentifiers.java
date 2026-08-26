package com.naturalist.usage;

import java.util.UUID;

/**
 * Hardcoded identifier constants for deterministic usage repository test authoring.
 * <p>
 * {@code UsageCounters.Identification} is the single real seeded counter
 * ({@code identification}); a second fictitious-but-valid counter is provided for
 * set-based lookup tests. {@code NotFound} carries a fictitious {@link UsageCounterName}
 * plus fictitious {@link UsageTallyId}/{@link UsageAlertId} values guaranteed never to
 * appear in any JSON catalog, for empty-result assertions.
 * <p>
 * Usage in contract tests:
 * <pre>
 *     repository().getByName(TestUsageIdentifiers.UsageCounters.Identification)
 *     repository().getByName(TestUsageIdentifiers.UsageCounters.NotFound.name)
 * </pre>
 */
public class TestUsageIdentifiers {

    private TestUsageIdentifiers() {
    }

    public static class UsageCounters {

        private UsageCounters() {
        }

        /** The single real seeded counter — see {@code usage/usage-counters.json}. */
        public static final UsageCounterName Identification = UsageCounterName.of("identification");

        /** Fictitious but valid second counter for set-based lookup tests. */
        public static final UsageCounterName InsectIdentification = UsageCounterName.of("insect-identification");

        /**
         * Fictitious identifier for the {@link com.naturalist.usage.UsageCounterName}
         * scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final UsageCounterName name = UsageCounterName.of("unobtainium-counter");
        }
    }

    public static class UsageTallies {

        private UsageTallies() {
        }

        /**
         * Fictitious identifier for the {@link com.naturalist.usage.UsageTallyId}
         * scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final UsageTallyId id = UsageTallyId.of(
                    UUID.fromString("019dbdb8-1a11-7eee-1a11-1a11a11a11a1"));
        }
    }

    public static class UsageAlerts {

        private UsageAlerts() {
        }

        /**
         * Fictitious identifier for the {@link com.naturalist.usage.UsageAlertId}
         * scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final UsageAlertId id = UsageAlertId.of(
                    UUID.fromString("019dbdb8-2a22-7eee-2a22-2a22a22a22a2"));
        }
    }
}
