package com.naturalist.data;

import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Constraints;
import com.naturalist.exception.InvariantViolationException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * A repository read is a producer (ADR-017): it checks its arguments and hands on what it read,
 * unobserved and never thrown on (J4). An invalid entity read from storage reaches the consumer,
 * which observes it and decides whether its invalid aspects matter; the read records no
 * {@code kind=state} observation of its own.
 *
 * <p>No consumer exercises this edge: every {@code TestEntitySource} validates on insert, so an
 * invalid stored entity only reaches a read through the rdbms adapters' real tables.
 */
class AbstractEntityRepositoryReadObservationTest {

    private static final String CHECK = "naturalist.invariant.check";

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final SpecimenRepository repository = new SpecimenRepository();

    private static final Specimen VALID = new Specimen(new SpecimenName("monarch"), "Danaus plexippus");
    private static final Specimen INVALID = new Specimen(new SpecimenName("unlabelled"), " ");

    @BeforeEach
    void bindMeters() {
        Metrics.addRegistry(meters);
        repository.store(VALID);
        repository.store(INVALID);
    }

    @AfterEach
    void unbindMeters() {
        Metrics.removeRegistry(meters);
    }

    private double stateObservations(String method) {
        return observations(method, "state");
    }

    private double observations(String method, String kind) {
        return meters.find(CHECK)
                .tag("class", "SpecimenRepository")
                .tag("method", method)
                .tag("kind", kind)
                .counters().stream()
                .mapToDouble(Counter::count)
                .sum();
    }

    @Test
    void getByName_handsOnTheInvalidEntityUnobserved() {
        assertThat(repository.getByName(INVALID.name())).contains(INVALID);
        assertThat(stateObservations("getByName")).isZero();
    }

    @Test
    void getByEntityNameSet_handsOnTheInvalidEntityUnobserved() {
        assertThat(repository.getByEntityNameSet(Set.of(VALID.name(), INVALID.name())))
                .containsExactlyInAnyOrder(VALID, INVALID);
        assertThat(stateObservations("getByEntityNameSet")).isZero();
    }

    @Test
    void getPage_handsOnTheInvalidEntityUnobserved() {
        assertThat(repository.getPage(PageRequest.first(10)).content())
                .containsExactly(VALID, INVALID);
        assertThat(stateObservations("getPage")).isZero();
    }

    @Test
    void getByName_nullNameIsRefusedAndObservedUnderTheConcreteRepository() {
        assertThat(catchThrowable(() -> repository.getByName(null)))
                .isInstanceOf(InvariantViolationException.class);
        assertThat(observations("getByName", "argument")).isPositive();
    }

    static final class SpecimenName extends EntityName {
        SpecimenName(String value) {
            super(value);
        }

        @Override
        protected int maxLength() {
            return 64;
        }
    }

    record Specimen(SpecimenName name, String label) implements NamedEntity<SpecimenName> {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return c -> c.entityName(name, "name").notBlank(label, "label");
        }
    }

    /** Storage that, like a database table, holds whatever was written to it. */
    static final class SpecimenRepository extends AbstractEntityRepository<SpecimenName, Specimen> {
        private final Map<SpecimenName, Specimen> rows = new LinkedHashMap<>();

        void store(Specimen specimen) {
            rows.put(specimen.name(), specimen);
        }

        @Override
        protected Optional<Specimen> doGetByName(SpecimenName name) {
            return Optional.ofNullable(rows.get(name));
        }

        @Override
        protected List<Specimen> doGetByNameSet(Set<SpecimenName> nameSet) {
            return rows.values().stream().filter(s -> nameSet.contains(s.name())).toList();
        }

        @Override
        protected Page<Specimen> doGetPage(PageRequest pageRequest) {
            return new Page<>(List.copyOf(rows.values()), pageRequest.pageNumber(), pageRequest.pageSize(), 0, false);
        }

        @Override
        protected void doInsert(Specimen entity) {
            store(entity);
        }

        @Override
        protected void doUpdate(Specimen entity) {
            store(entity);
        }

        @Override
        protected Specimen doSave(Specimen entity) {
            store(entity);
            return entity;
        }
    }
}
