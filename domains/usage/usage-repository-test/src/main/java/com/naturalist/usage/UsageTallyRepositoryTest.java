package com.naturalist.usage;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link UsageRepository.TallyRepository}. Inherits the
 * {@link EntityRepositoryTest} cases (ADR-002).
 *
 * <p>{@code usage/usage-tallies.json} seeds no rows — tallies accrue at runtime
 * (per {@code UsageTallyTestEntitySourceTest}'s own doc comment) — so two known
 * fixture tallies are inserted into {@link #source()} before each test rather than
 * sourced from a pre-seeded catalog.
 */
interface UsageTallyRepositoryTest extends EntityRepositoryTest<UsageTallyId, UsageTally> {

    UsageTallyId KnownTally1Id = UsageTallyId.of(UUID.fromString("019dbdb8-5a55-7eee-5a55-5a55a55a55a5"));
    UsageTallyId KnownTally2Id = UsageTallyId.of(UUID.fromString("019dbdb8-6a66-7eee-6a66-6a66a66a66a6"));

    NaturalistName KnownNaturalist = NaturalistName.of("patrick-way");

    @Override
    UsageRepository.TallyRepository repository();

    @Override
    default TestEntitySource<UsageTallyId, UsageTally> source() {
        return db.getNamed(UsageTallyTestEntitySource.class);
    }

    @BeforeEach
    default void seedKnownTallies() {
        source().insert(new UsageTally(
                KnownTally1Id,
                TestUsageIdentifiers.UsageCounters.Identification,
                KnownNaturalist,
                "daily-2026-08-25",
                5));
        source().insert(new UsageTally(
                KnownTally2Id,
                TestUsageIdentifiers.UsageCounters.Identification,
                null,
                "rate-2026-08-25-14-30",
                3));
    }

    @Override
    default UsageTallyId notFoundName() {
        return TestUsageIdentifiers.UsageTallies.NotFound.id;
    }

    @Override
    default List<UsageTallyId> knownEntityNames() {
        return List.of(KnownTally1Id, KnownTally2Id);
    }

    @Override
    default UsageTally newEntity() {
        return new UsageTally(
                UsageTallyId.create(),
                TestUsageIdentifiers.UsageCounters.Identification,
                NaturalistName.of("someone-else"),
                "daily-2026-08-26",
                1);
    }

    @Override
    default UsageTally ghostEntity() {
        return new UsageTally(
                TestUsageIdentifiers.UsageTallies.NotFound.id,
                TestUsageIdentifiers.UsageCounters.Identification,
                null,
                "daily-2026-09-01",
                0);
    }

    @Override
    default UsageTally modifiedEntity(UsageTally original) {
        return original.withCount(original.count() + 100);
    }

    // =========================================================================
    // findBusinessKey
    // =========================================================================

    @Test
    default void findBusinessKey_nullCounter_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().findBusinessKey(null, KnownNaturalist, "daily-2026-08-25"))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("counter");
    }

    @Test
    default void findBusinessKey_nullPeriod_throwsInvariantViolationException() {
        assertThatThrownBy(() -> repository().findBusinessKey(
                TestUsageIdentifiers.UsageCounters.Identification, KnownNaturalist, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("period");
    }

    @Test
    default void findBusinessKey_noMatch_returnsEmpty() {
        Optional<UsageTally> result = repository().findBusinessKey(
                TestUsageIdentifiers.UsageCounters.Identification, KnownNaturalist, "daily-2099-01-01");

        assertThat(result).isEmpty();
    }

    @Test
    default void findBusinessKey_knownPerUserTally_returnsIt() {
        Optional<UsageTally> result = repository().findBusinessKey(
                TestUsageIdentifiers.UsageCounters.Identification, KnownNaturalist, "daily-2026-08-25");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(KnownTally1Id);
    }

    @Test
    default void findBusinessKey_nullNaturalistMatchesGlobalTally() {
        Optional<UsageTally> result = repository().findBusinessKey(
                TestUsageIdentifiers.UsageCounters.Identification, null, "rate-2026-08-25-14-30");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(KnownTally2Id);
    }

    @Test
    default void findBusinessKey_naturalistMismatch_returnsEmpty() {
        Optional<UsageTally> result = repository().findBusinessKey(
                TestUsageIdentifiers.UsageCounters.Identification,
                NaturalistName.of("a-different-naturalist"),
                "daily-2026-08-25");

        assertThat(result).isEmpty();
    }
}
