package com.naturalist.usage;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

/**
 * Behavioral contract for {@link UsageRepository.CounterRepository}. Inherits the
 * {@link EntityRepositoryTest} cases (ADR-002).
 *
 * <p>{@code usage/usage-counters.json} seeds only the one real counter
 * ({@code identification}) — the set-based query cases need a second, distinct known
 * name, so {@link TestUsageIdentifiers.UsageCounters#InsectIdentification} (documented
 * there as "fictitious but valid ... for set-based lookup tests") is inserted into the
 * backing {@link #source()} before each test, mirroring the round-trip-insert pattern
 * {@code UsageCounterTestEntitySourceTest#insertedCounterIsRetrievable} already
 * established for that same constant.
 */
interface UsageCounterRepositoryTest extends EntityRepositoryTest<UsageCounterName, UsageCounter> {

    @Override
    UsageRepository.CounterRepository repository();

    @Override
    default TestEntitySource<UsageCounterName, UsageCounter> source() {
        return db.getNamed(UsageCounterTestEntitySource.class);
    }

    @BeforeEach
    default void seedSecondKnownCounter() {
        source().insert(new UsageCounter(TestUsageIdentifiers.UsageCounters.InsectIdentification));
    }

    @Override
    default UsageCounterName notFoundName() {
        return TestUsageIdentifiers.UsageCounters.NotFound.name;
    }

    @Override
    default List<UsageCounterName> knownEntityNames() {
        return List.of(
                TestUsageIdentifiers.UsageCounters.Identification,
                TestUsageIdentifiers.UsageCounters.InsectIdentification);
    }

    @Override
    default UsageCounter newEntity() {
        return new UsageCounter(UsageCounterName.of("test-new-counter"));
    }

    @Override
    default UsageCounter ghostEntity() {
        return new UsageCounter(UsageCounterName.of("test-ghost-counter"));
    }

    /**
     * {@link UsageCounter} carries only its own {@code name} — no other mutable
     * field exists to vary, so the "modified" entity is structurally identical to
     * {@code original}. The update contract case still exercises the write path
     * (an update-in-place round trip), it just cannot assert on a changed value.
     */
    @Override
    default UsageCounter modifiedEntity(UsageCounter original) {
        return original;
    }
}
