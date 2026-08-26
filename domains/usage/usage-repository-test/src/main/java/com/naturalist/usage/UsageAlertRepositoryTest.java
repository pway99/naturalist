package com.naturalist.usage;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link UsageRepository.AlertRepository}. Inherits the
 * {@link EntityRepositoryTest} cases (ADR-002).
 *
 * <p>{@code usage/usage-alerts.json} seeds no rows — alerts are raised at runtime
 * (per {@code UsageAlertTestEntitySourceTest}'s own doc comment) — so two known
 * fixture alerts are inserted into {@link #source()} before each test rather than
 * sourced from a pre-seeded catalog.
 */
interface UsageAlertRepositoryTest extends EntityRepositoryTest<UsageAlertId, UsageAlert> {

    UsageAlertId KnownAlert1Id = UsageAlertId.of(UUID.fromString("019dbdb8-7a77-7eee-7a77-7a77a77a77a7"));
    UsageAlertId KnownAlert2Id = UsageAlertId.of(UUID.fromString("019dbdb8-8a88-7eee-8a88-8a88a88a88a8"));

    @Override
    UsageRepository.AlertRepository repository();

    @Override
    default TestEntitySource<UsageAlertId, UsageAlert> source() {
        return db.getNamed(UsageAlertTestEntitySource.class);
    }

    @BeforeEach
    default void seedKnownAlerts() {
        source().insert(new UsageAlert(
                KnownAlert1Id,
                TestUsageIdentifiers.UsageCounters.Identification,
                AlertScope.DAILY,
                AlertKind.WARNING,
                "daily-2026-08-25",
                "80% of daily identification budget used",
                Instant.parse("2026-08-25T10:00:00Z"),
                false,
                false));
        source().insert(new UsageAlert(
                KnownAlert2Id,
                TestUsageIdentifiers.UsageCounters.Identification,
                AlertScope.MONTHLY,
                AlertKind.HARD_STOP,
                "monthly-2026-08",
                "100% of monthly identification budget used",
                Instant.parse("2026-08-25T14:00:00Z"),
                true,
                true));
    }

    @Override
    default UsageAlertId notFoundName() {
        return TestUsageIdentifiers.UsageAlerts.NotFound.id;
    }

    @Override
    default List<UsageAlertId> knownEntityNames() {
        return List.of(KnownAlert1Id, KnownAlert2Id);
    }

    @Override
    default UsageAlert newEntity() {
        return new UsageAlert(
                UsageAlertId.create(),
                TestUsageIdentifiers.UsageCounters.Identification,
                AlertScope.DAILY,
                AlertKind.WARNING,
                "daily-2026-08-27",
                "new alert",
                Instant.parse("2026-08-27T09:00:00Z"),
                false,
                false);
    }

    @Override
    default UsageAlert ghostEntity() {
        return new UsageAlert(
                TestUsageIdentifiers.UsageAlerts.NotFound.id,
                TestUsageIdentifiers.UsageCounters.Identification,
                AlertScope.DAILY,
                AlertKind.WARNING,
                "daily-2026-09-01",
                "ghost alert",
                Instant.parse("2026-08-25T09:00:00Z"),
                false,
                false);
    }

    @Override
    default UsageAlert modifiedEntity(UsageAlert original) {
        return original.withEmailed(!original.emailed()).withAcknowledged(!original.acknowledged());
    }

    // =========================================================================
    // findDedupKey
    // =========================================================================

    @Test
    default void findDedupKey_nullCounter_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().findDedupKey(
                null, AlertScope.DAILY, AlertKind.WARNING, "daily-2026-08-25"))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("counter");
    }

    @Test
    default void findDedupKey_nullScope_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().findDedupKey(
                TestUsageIdentifiers.UsageCounters.Identification, null, AlertKind.WARNING, "daily-2026-08-25"))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("scope");
    }

    @Test
    default void findDedupKey_nullKind_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().findDedupKey(
                TestUsageIdentifiers.UsageCounters.Identification, AlertScope.DAILY, null, "daily-2026-08-25"))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("kind");
    }

    @Test
    default void findDedupKey_nullPeriod_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().findDedupKey(
                TestUsageIdentifiers.UsageCounters.Identification, AlertScope.DAILY, AlertKind.WARNING, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("period");
    }

    @Test
    default void findDedupKey_noMatch_returnsEmpty() {
        Optional<UsageAlert> result = repository().findDedupKey(
                TestUsageIdentifiers.UsageCounters.Identification, AlertScope.DAILY, AlertKind.WARNING, "daily-2099-01-01");

        assertThat(result).isEmpty();
    }

    @Test
    default void findDedupKey_knownAlert_returnsIt() {
        Optional<UsageAlert> result = repository().findDedupKey(
                TestUsageIdentifiers.UsageCounters.Identification, AlertScope.DAILY, AlertKind.WARNING, "daily-2026-08-25");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(KnownAlert1Id);
    }

    // =========================================================================
    // getUnacknowledged
    // =========================================================================

    @Test
    default void getUnacknowledged_excludesAcknowledgedAlerts() {
        List<UsageAlert> result = repository().getUnacknowledged();

        assertThat(result).extracting(UsageAlert::id).doesNotContain(KnownAlert2Id);
    }

    @Test
    default void getUnacknowledged_isOrderedNewestFirst() {
        UsageAlertId laterId = UsageAlertId.create();
        source().insert(new UsageAlert(
                laterId,
                TestUsageIdentifiers.UsageCounters.Identification,
                AlertScope.DAILY,
                AlertKind.WARNING,
                "daily-2026-08-26",
                "a later, still-unacknowledged warning",
                Instant.parse("2026-08-25T12:00:00Z"),
                false,
                false));

        List<UsageAlert> result = repository().getUnacknowledged();

        assertThat(result).extracting(UsageAlert::id).containsExactly(laterId, KnownAlert1Id);
    }

    @Test
    default void getUnacknowledged_noneUnacknowledged_returnsEmpty() {
        source().update(source().getByName(KnownAlert1Id).orElseThrow().withAcknowledged(true));

        List<UsageAlert> result = repository().getUnacknowledged();

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // getUnsent
    // =========================================================================

    @Test
    default void getUnsent_excludesEmailedAlerts() {
        List<UsageAlert> result = repository().getUnsent();

        assertThat(result).extracting(UsageAlert::id)
                .contains(KnownAlert1Id)
                .doesNotContain(KnownAlert2Id);
    }

    @Test
    default void getUnsent_noneUnsent_returnsEmpty() {
        source().update(source().getByName(KnownAlert1Id).orElseThrow().withEmailed(true));

        List<UsageAlert> result = repository().getUnsent();

        assertThat(result).isEmpty();
    }
}
