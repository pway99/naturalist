package com.naturalist.usage;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link UsageRepository.EventRepository}. Inherits the
 * {@link EntityRepositoryTest} cases (ADR-002).
 *
 * <p>{@code usage/usage-events.json} seeds no rows — events are appended at runtime
 * (per {@code UsageEventTestEntitySourceTest}'s own doc comment) — so two known
 * fixture events are inserted into {@link #source()} before each test rather than
 * sourced from a pre-seeded catalog.
 */
interface UsageEventRepositoryTest extends EntityRepositoryTest<UsageEventId, UsageEvent> {

    NaturalistName KnownNaturalist = NaturalistName.of("patrick-way");
    NaturalistName OtherNaturalist = NaturalistName.of("someone-else");

    @Override
    UsageRepository.EventRepository repository();

    @Override
    default TestEntitySource<UsageEventId, UsageEvent> source() {
        return db.getNamed(UsageEventTestEntitySource.class);
    }

    @BeforeEach
    default void seedKnownEvents() {
        source().insert(new UsageEvent(
                TestUsageIdentifiers.UsageEvents.Known1,
                TestUsageIdentifiers.UsageCounters.Identification,
                KnownNaturalist,
                Instant.parse("2026-08-25T10:00:00Z")));
        source().insert(new UsageEvent(
                TestUsageIdentifiers.UsageEvents.Known2,
                TestUsageIdentifiers.UsageCounters.Identification,
                OtherNaturalist,
                Instant.parse("2026-08-25T11:00:00Z")));
    }

    @Override
    default UsageEventId notFoundName() {
        return TestUsageIdentifiers.UsageEvents.NotFound.id;
    }

    @Override
    default List<UsageEventId> knownEntityNames() {
        return List.of(TestUsageIdentifiers.UsageEvents.Known1, TestUsageIdentifiers.UsageEvents.Known2);
    }

    @Override
    default UsageEvent newEntity() {
        return new UsageEvent(
                UsageEventId.create(),
                TestUsageIdentifiers.UsageCounters.Identification,
                OtherNaturalist,
                Instant.parse("2026-08-26T09:00:00Z"));
    }

    @Override
    default UsageEvent ghostEntity() {
        return new UsageEvent(
                TestUsageIdentifiers.UsageEvents.NotFound.id,
                TestUsageIdentifiers.UsageCounters.Identification,
                KnownNaturalist,
                Instant.parse("2026-08-26T09:00:00Z"));
    }

    /**
     * {@link UsageEvent} is append-only and immutable — no field is ever mutated
     * after creation, so the "modified" entity is structurally identical to
     * {@code original}. The update contract case still exercises the write path
     * (an update-in-place round trip), it just cannot assert on a changed value —
     * mirrors {@code UsageCounterRepositoryTest#modifiedEntity}.
     */
    @Override
    default UsageEvent modifiedEntity(UsageEvent original) {
        return original;
    }

    // =========================================================================
    // findByCounterSince
    // =========================================================================

    @Test
    default void findByCounterSince_nullCounter_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().findByCounterSince(
                null, null, Instant.parse("2026-08-25T00:00:00Z")))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("counter");
    }

    @Test
    default void findByCounterSince_nullSince_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().findByCounterSince(
                TestUsageIdentifiers.UsageCounters.Identification, null, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("since");
    }

    @Test
    default void findByCounterSince_beforeWindow_returnsEmpty() {
        List<UsageEvent> result = repository().findByCounterSince(
                TestUsageIdentifiers.UsageCounters.Identification,
                null,
                Instant.parse("2026-08-26T00:00:00Z"));

        assertThat(result).isEmpty();
    }

    @Test
    default void findByCounterSince_nullNaturalist_returnsAllInWindow() {
        List<UsageEvent> result = repository().findByCounterSince(
                TestUsageIdentifiers.UsageCounters.Identification,
                null,
                Instant.parse("2026-08-25T00:00:00Z"));

        assertThat(result)
                .extracting(UsageEvent::id)
                .containsExactlyInAnyOrder(
                        TestUsageIdentifiers.UsageEvents.Known1, TestUsageIdentifiers.UsageEvents.Known2);
    }

    @Test
    default void findByCounterSince_withNaturalist_returnsOnlyThatNaturalist() {
        List<UsageEvent> result = repository().findByCounterSince(
                TestUsageIdentifiers.UsageCounters.Identification,
                KnownNaturalist,
                Instant.parse("2026-08-25T00:00:00Z"));

        assertThat(result)
                .extracting(UsageEvent::id)
                .containsExactly(TestUsageIdentifiers.UsageEvents.Known1);
    }

    @Test
    default void findByCounterSince_sinceIsInclusive() {
        List<UsageEvent> result = repository().findByCounterSince(
                TestUsageIdentifiers.UsageCounters.Identification,
                null,
                Instant.parse("2026-08-25T10:00:00Z"));

        assertThat(result)
                .extracting(UsageEvent::id)
                .contains(TestUsageIdentifiers.UsageEvents.Known1);
    }
}
