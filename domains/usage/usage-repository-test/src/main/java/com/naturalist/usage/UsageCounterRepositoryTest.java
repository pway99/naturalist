package com.naturalist.usage;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link UsageRepository.CounterRepository}. Inherits the
 * {@link EntityRepositoryTest} cases (ADR-002).
 *
 * <p>{@code usage/usage-counters.json} seeds three rules, all for the
 * {@code identification} counter activity: a per-user daily rule, a global daily
 * rule, and a global monthly ({@link WindowKind#SINCE}) rule.
 */
interface UsageCounterRepositoryTest extends EntityRepositoryTest<UsageCounterId, UsageCounter> {

    @Override
    UsageRepository.CounterRepository repository();

    @Override
    default TestEntitySource<UsageCounterId, UsageCounter> source() {
        return db.getNamed(UsageCounterTestEntitySource.class);
    }

    @Override
    default UsageCounterId notFoundName() {
        return TestUsageIdentifiers.UsageCounters.NotFound.id;
    }

    @Override
    default List<UsageCounterId> knownEntityNames() {
        return List.of(
                TestUsageIdentifiers.UsageCounters.PerUserDailyId,
                TestUsageIdentifiers.UsageCounters.GlobalDailyId,
                TestUsageIdentifiers.UsageCounters.GlobalMonthlyId);
    }

    @Override
    default UsageCounter newEntity() {
        return new UsageCounter(
                UsageCounterId.create(),
                TestUsageIdentifiers.UsageCounters.Identification,
                UsageScope.PER_USER,
                WindowKind.SINCE,
                Instant.parse("2026-08-01T00:00:00Z"),
                100,
                true);
    }

    @Override
    default UsageCounter ghostEntity() {
        return new UsageCounter(
                TestUsageIdentifiers.UsageCounters.NotFound.id,
                TestUsageIdentifiers.UsageCounters.Identification,
                UsageScope.PER_USER,
                WindowKind.CALENDAR_DAY,
                null,
                5,
                true);
    }

    @Override
    default UsageCounter modifiedEntity(UsageCounter original) {
        return original.withLimit(original.limit() + 5).withActive(!original.active());
    }

    // =========================================================================
    // findByCounterName
    // =========================================================================

    @Test
    default void findByCounterName_nullArgument_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().findByCounterName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("counterName");
    }

    @Test
    default void findByCounterName_unknownName_returnsEmpty() {
        List<UsageCounter> result = repository().findByCounterName(
                TestUsageIdentifiers.UsageCounters.InsectIdentification);

        assertThat(result).isEmpty();
    }

    @Test
    default void findByCounterName_knownName_returnsAllRulesForThatActivity() {
        List<UsageCounter> result = repository().findByCounterName(
                TestUsageIdentifiers.UsageCounters.Identification);

        assertThat(result).hasSize(3);
    }
}
