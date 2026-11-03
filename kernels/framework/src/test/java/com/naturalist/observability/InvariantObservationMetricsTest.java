package com.naturalist.observability;

import com.naturalist.exception.InvariantViolationException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The metric contract of {@link InvariantObservation}: every inspected constraint is one
 * increment of {@code naturalist.invariant.check}, tagged {@code outcome=held|violated}.
 * {@link Observer.MonitoringMode#ON_FAILURE} emits the violated checks only;
 * {@link Observer.MonitoringMode#ALWAYS} emits the held checks too — never a second meter.
 */
class InvariantObservationMetricsTest {

    private static final String CHECK = "naturalist.invariant.check";
    private static final String PRESENT = "InvariantObservationMetricsTest.register.present";
    private static final String ABSENT = "InvariantObservationMetricsTest.register.absent";

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    @BeforeEach
    void bindMeters() {
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void unbindMeters() {
        Metrics.removeRegistry(meters);
    }

    private static InvariantObservation oneHeldOneViolated(Observer.MonitoringMode mode) {
        return Observer.forClass(InvariantObservationMetricsTest.class, mode)
                .arguments("register", i -> i.notNull("value", "present").notNull(null, "absent"));
    }

    private Counter check(String name, String outcome) {
        return meters.find(CHECK).tag("name", name).tag("outcome", outcome).counter();
    }

    private double totalIncrements() {
        return meters.getMeters().stream()
                .filter(m -> m instanceof Counter)
                .mapToDouble(m -> ((Counter) m).count())
                .sum();
    }

    @Test
    void onFailure_emitsOnlyTheViolatedCheck() {
        oneHeldOneViolated(Observer.MonitoringMode.ON_FAILURE).observe(Level.WARN);

        assertThat(check(ABSENT, "violated").count()).isEqualTo(1);
        // The global composite replays meters other tests registered into this registry at
        // zero, so "not emitted" is a zero count, not a missing meter.
        Counter held = check(PRESENT, "held");
        assertThat(held == null ? 0 : held.count()).isZero();
    }

    @Test
    void always_emitsHeldAndViolatedChecks() {
        oneHeldOneViolated(Observer.MonitoringMode.ALWAYS).observe(Level.WARN);

        assertThat(check(ABSENT, "violated").count()).isEqualTo(1);
        assertThat(check(PRESENT, "held").count()).isEqualTo(1);
    }

    @Test
    void always_countsEachConstraintExactlyOnce() {
        oneHeldOneViolated(Observer.MonitoringMode.ALWAYS).observe(Level.WARN);

        // Two constraints inspected, two increments: a violation is not also counted elsewhere.
        assertThat(totalIncrements()).isEqualTo(2);
        assertThat(meters.getMeters()).allMatch(m -> m.getId().getName().equals(CHECK));
    }

    @Test
    void throwWhenInvalid_tagsViolatedErrorAndHeldInfo() {
        assertThatThrownBy(() -> oneHeldOneViolated(Observer.MonitoringMode.ALWAYS).throwWhenInvalid())
                .isInstanceOf(InvariantViolationException.class);

        assertThat(check(ABSENT, "violated").getId().getTag("level")).isEqualTo("error");
        assertThat(check(PRESENT, "held").getId().getTag("level")).isEqualTo("info");
    }

    @Test
    void observe_tagsViolatedAtTheCallSiteLevelAndHeldInfo() {
        oneHeldOneViolated(Observer.MonitoringMode.ALWAYS).observe(Level.WARN);

        assertThat(check(ABSENT, "violated").getId().getTag("level")).isEqualTo("warn");
        assertThat(check(PRESENT, "held").getId().getTag("level")).isEqualTo("info");
    }

    @Test
    void arguments_tagsKindArgument() {
        oneHeldOneViolated(Observer.MonitoringMode.ALWAYS).observe(Level.WARN);

        assertThat(check(ABSENT, "violated").getId().getTag("kind")).isEqualTo("argument");
        assertThat(check(PRESENT, "held").getId().getTag("kind")).isEqualTo("argument");
    }

    @Test
    void state_tagsKindState() {
        Observer.forClass(InvariantObservationMetricsTest.class)
                .state("acquire", s -> s.notNull(null, "owner"))
                .observe(Level.WARN);

        Counter counter = check("InvariantObservationMetricsTest.acquire.owner", "violated");
        assertThat(counter.count()).isEqualTo(1);
        assertThat(counter.getId().getTag("kind")).isEqualTo("state");
    }

    @Test
    void state_throwWhenInvalid_throwsAndTagsKindState() {
        assertThatThrownBy(() -> Observer.forClass(InvariantObservationMetricsTest.class)
                .state("count", s -> s.atLeast(0, 1, "plots"))
                .throwWhenInvalid())
                .isInstanceOf(InvariantViolationException.class);

        Counter counter = check("InvariantObservationMetricsTest.count.plots", "violated");
        assertThat(counter.getId().getTag("kind")).isEqualTo("state");
        assertThat(counter.getId().getTag("level")).isEqualTo("error");
    }

    @Test
    void methodObserverState_tagsKindState() {
        Observer.forClass(InvariantObservationMetricsTest.class)
                .forMethod("lookup")
                .state(s -> s.notBlank("", "handle"))
                .observe(Level.WARN);

        Counter counter = check("InvariantObservationMetricsTest.lookup.handle", "violated");
        assertThat(counter.getId().getTag("kind")).isEqualTo("state");
    }

    @Test
    void observable_tagsKindObservable() {
        Observer.forClass(InvariantObservationMetricsTest.class)
                .observable(null, "fetched")
                .observe(Level.WARN);

        Counter counter = check("InvariantObservationMetricsTest.fetched.fetched", "violated");
        assertThat(counter.getId().getTag("kind")).isEqualTo("observable");
    }
}
