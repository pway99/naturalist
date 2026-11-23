package com.naturalist.observability;

import com.naturalist.exception.InvariantViolationException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.UncheckedIOException;
import java.io.IOException;

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

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    @BeforeEach
    void bindMeters() {
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void unbindMeters() {
        Metrics.removeRegistry(meters);
    }

    /** One held and one violated argument check, observed under the calling test's own method name. */
    private static InvariantObservation oneHeldOneViolated(Observer.MonitoringMode mode, String testMethod) {
        return Observer.forClass(InvariantObservationMetricsTest.class, mode).forMethod(testMethod)
                .arguments(i -> i.notNull("value", "present").notNull(null, "absent"));
    }

    private static String checkName(String testMethod, String constraint) {
        return "InvariantObservationMetricsTest." + testMethod + "." + constraint;
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
        oneHeldOneViolated(Observer.MonitoringMode.ON_FAILURE, "onFailure_emitsOnlyTheViolatedCheck").observe(Level.WARN);

        assertThat(check(checkName("onFailure_emitsOnlyTheViolatedCheck", "absent"), "violated").count()).isEqualTo(1);
        // The global composite replays meters other tests registered into this registry at
        // zero, so "not emitted" is a zero count, not a missing meter.
        Counter held = check(checkName("onFailure_emitsOnlyTheViolatedCheck", "present"), "held");
        assertThat(held == null ? 0 : held.count()).isZero();
    }

    @Test
    void always_emitsHeldAndViolatedChecks() {
        oneHeldOneViolated(Observer.MonitoringMode.ALWAYS, "always_emitsHeldAndViolatedChecks").observe(Level.WARN);

        assertThat(check(checkName("always_emitsHeldAndViolatedChecks", "absent"), "violated").count()).isEqualTo(1);
        assertThat(check(checkName("always_emitsHeldAndViolatedChecks", "present"), "held").count()).isEqualTo(1);
    }

    @Test
    void always_countsEachConstraintExactlyOnce() {
        oneHeldOneViolated(Observer.MonitoringMode.ALWAYS, "always_countsEachConstraintExactlyOnce").observe(Level.WARN);

        // Two constraints inspected, two increments: a violation is not also counted elsewhere.
        assertThat(totalIncrements()).isEqualTo(2);
        assertThat(meters.getMeters()).allMatch(m -> m.getId().getName().equals(CHECK));
    }

    @Test
    void throwWhenInvalid_tagsViolatedErrorAndHeldInfo() {
        assertThatThrownBy(() -> oneHeldOneViolated(Observer.MonitoringMode.ALWAYS, "throwWhenInvalid_tagsViolatedErrorAndHeldInfo").throwWhenInvalid())
                .isInstanceOf(InvariantViolationException.class);

        assertThat(check(checkName("throwWhenInvalid_tagsViolatedErrorAndHeldInfo", "absent"), "violated").getId().getTag("level")).isEqualTo("error");
        assertThat(check(checkName("throwWhenInvalid_tagsViolatedErrorAndHeldInfo", "present"), "held").getId().getTag("level")).isEqualTo("info");
    }

    @Test
    void observe_tagsViolatedAtTheCallSiteLevelAndHeldInfo() {
        oneHeldOneViolated(Observer.MonitoringMode.ALWAYS, "observe_tagsViolatedAtTheCallSiteLevelAndHeldInfo").observe(Level.WARN);

        assertThat(check(checkName("observe_tagsViolatedAtTheCallSiteLevelAndHeldInfo", "absent"), "violated").getId().getTag("level")).isEqualTo("warn");
        assertThat(check(checkName("observe_tagsViolatedAtTheCallSiteLevelAndHeldInfo", "present"), "held").getId().getTag("level")).isEqualTo("info");
    }

    @Test
    void arguments_tagsKindArgument() {
        oneHeldOneViolated(Observer.MonitoringMode.ALWAYS, "arguments_tagsKindArgument").observe(Level.WARN);

        assertThat(check(checkName("arguments_tagsKindArgument", "absent"), "violated").getId().getTag("kind")).isEqualTo("argument");
        assertThat(check(checkName("arguments_tagsKindArgument", "present"), "held").getId().getTag("kind")).isEqualTo("argument");
    }

    @Test
    void state_tagsKindState() {
        Observer.forClass(InvariantObservationMetricsTest.class)
                .state("state_tagsKindState", s -> s.notNull(null, "owner"))
                .observe(Level.WARN);

        Counter counter = check("InvariantObservationMetricsTest.state_tagsKindState.owner", "violated");
        assertThat(counter.count()).isEqualTo(1);
        assertThat(counter.getId().getTag("kind")).isEqualTo("state");
    }

    @Test
    void state_throwWhenInvalid_throwsAndTagsKindState() {
        assertThatThrownBy(() -> Observer.forClass(InvariantObservationMetricsTest.class)
                .state("state_throwWhenInvalid_throwsAndTagsKindState", s -> s.atLeast(0, 1, "plots"))
                .throwWhenInvalid())
                .isInstanceOf(InvariantViolationException.class);

        Counter counter = check("InvariantObservationMetricsTest.state_throwWhenInvalid_throwsAndTagsKindState.plots", "violated");
        assertThat(counter.getId().getTag("kind")).isEqualTo("state");
        assertThat(counter.getId().getTag("level")).isEqualTo("error");
    }

    @Test
    void methodObserverState_tagsKindState() {
        Observer.forClass(InvariantObservationMetricsTest.class)
                .forMethod("methodObserverState_tagsKindState")
                .state(s -> s.notBlank("", "handle"))
                .observe(Level.WARN);

        Counter counter = check("InvariantObservationMetricsTest.methodObserverState_tagsKindState.handle", "violated");
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

    /**
     * A caught failure is one {@code kind=caught} violation at ERROR on the same meter, tagged with
     * the exception's simple class name and with how the catch handled it.
     */
    @Test
    void noticeError_countsOneErrorViolation_taggedWithTheException() {
        Observer observer = Observer.forClass(InvariantObservationMetricsTest.class);
        observer.forMethod("noticeError_countsOneErrorViolation_taggedWithTheException").noticeError("stored", new UncheckedIOException(new IOException("disk")), Handling.RETHROWN);
        observer.noticeError("noticeError_countsOneErrorViolation_taggedWithTheException", "stored", new IllegalStateException("boom"), Handling.WRAPPED);

        Counter io = meters.find(CHECK).tag("name", "InvariantObservationMetricsTest.noticeError_countsOneErrorViolation_taggedWithTheException.stored")
                .tag("class", "InvariantObservationMetricsTest")
                .tag("method", "noticeError_countsOneErrorViolation_taggedWithTheException")
                .tag("exception", "UncheckedIOException")
                .tag("handling", "rethrown")
                .tag("outcome", "violated")
                .tag("level", "error")
                .tag("kind", "caught")
                .counter();
        assertThat(io).isNotNull();
        assertThat(io.count()).isEqualTo(1);
        Counter state = meters.find(CHECK).tag("name", "InvariantObservationMetricsTest.noticeError_countsOneErrorViolation_taggedWithTheException.stored")
                .tag("exception", "IllegalStateException")
                .tag("handling", "wrapped")
                .tag("kind", "caught")
                .counter();
        assertThat(state).isNotNull();
        assertThat(state.count()).isEqualTo(1);
    }

    /**
     * Counting a caught failure never throws, whatever it is handed: a null name, failure or
     * handling, or an anonymous exception class that has no simple name.
     */
    @Test
    void noticeError_neverThrows() {
        Observer observer = Observer.forClass(InvariantObservationMetricsTest.class);
        observer.noticeError(null, null, null, null);
        observer.forMethod("noticeError_neverThrows").noticeError(null, null, Handling.RETHROWN);
        observer.forMethod("noticeError_neverThrows").noticeError("anonymous", new RuntimeException() {
        }, Handling.RETHROWN);

        Counter anonymous = meters.find(CHECK).tag("name", "InvariantObservationMetricsTest.noticeError_neverThrows.anonymous")
                .tag("kind", "caught")
                .counter();
        assertThat(anonymous).isNotNull();
        assertThat(anonymous.getId().getTag("exception")).isNotBlank();
    }
}
