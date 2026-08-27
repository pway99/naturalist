package com.naturalist.usage;

import java.util.UUID;

/**
 * Hardcoded identifier constants for deterministic usage repository test authoring.
 * <p>
 * {@code UsageCounters.Identification} is the counter activity every seeded rule
 * targets; {@code UsageCounters.InsectIdentification} is a second fictitious-but-valid
 * activity for set-based lookup tests. {@code UsageCounters} is now an {@link
 * com.naturalist.ddd.Entity}-keyed rule catalog — {@code PerUserDailyId} /
 * {@code GlobalDailyId} / {@code GlobalMonthlyId} identify the three seeded rules by
 * {@link UsageCounterId}. {@code NotFound} carries fictitious {@link UsageCounterId} /
 * {@link UsageAlertId} values guaranteed never to appear in any JSON catalog, for
 * empty-result assertions.
 * <p>
 * Usage in contract tests:
 * <pre>
 *     repository().getByName(TestUsageIdentifiers.UsageCounters.PerUserDailyId)
 *     repository().getByName(TestUsageIdentifiers.UsageCounters.NotFound.id)
 * </pre>
 */
public class TestUsageIdentifiers {

    private TestUsageIdentifiers() {
    }

    public static class UsageCounters {

        private UsageCounters() {
        }

        /** The counter activity every seeded rule targets — see {@code usage/usage-counters.json}. */
        public static final UsageCounterName Identification = UsageCounterName.of("identification");

        /** Fictitious but valid second counter activity for set-based lookup tests. */
        public static final UsageCounterName InsectIdentification = UsageCounterName.of("insect-identification");

        /** Seeded per-user daily identification rule (limit 10). */
        public static final UsageCounterId PerUserDailyId = UsageCounterId.of(
                UUID.fromString("019dbdb9-0d01-7eee-0d01-0d010d010d01"));

        /** Seeded global daily identification rule (limit 50). */
        public static final UsageCounterId GlobalDailyId = UsageCounterId.of(
                UUID.fromString("019dbdb9-0d02-7eee-0d02-0d020d020d02"));

        /** Seeded global monthly (SINCE) identification rule (limit 650). */
        public static final UsageCounterId GlobalMonthlyId = UsageCounterId.of(
                UUID.fromString("019dbdb9-0d03-7eee-0d03-0d030d030d03"));

        /**
         * Fictitious identifier for the {@link com.naturalist.usage.UsageCounterId}
         * scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final UsageCounterId id = UsageCounterId.of(
                    UUID.fromString("019dbdb9-0d99-7eee-0d99-0d990d990d99"));
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

    public static class UsageEvents {

        private UsageEvents() {
        }

        public static final UsageEventId Known1 =
                UsageEventId.of(UUID.fromString("019dbdb8-3a33-7eee-3a33-3a33a33a33a3"));
        public static final UsageEventId Known2 =
                UsageEventId.of(UUID.fromString("019dbdb8-4a44-7eee-4a44-4a44a44a44a4"));

        /**
         * Fictitious identifier for the {@link com.naturalist.usage.UsageEventId}
         * scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final UsageEventId id = UsageEventId.of(
                    UUID.fromString("019dbdb8-5a55-7eee-5a55-5a55a55a55a5"));
        }
    }
}
