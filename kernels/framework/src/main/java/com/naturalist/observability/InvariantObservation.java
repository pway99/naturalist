package com.naturalist.observability;

import com.naturalist.exception.InvariantViolationException;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The result of a single observation pass over a constraint graph. Holds the flattened
 * set of constraints, each with a fully-qualified dotted-path name, and offers two
 * terminal operations:
 *
 * <ul>
 *   <li>{@link #throwWhenInvalid()} — emit metrics and throw
 *       {@link InvariantViolationException} if any constraint fails. Violated
 *       checks emitted from this path always carry {@code level=error} since
 *       an exception is about to be thrown.</li>
 *   <li>{@link #observe(Level)} — emit metrics only; no throw regardless of validity.
 *       The call site supplies the {@link Level} — only the call site knows
 *       whether the observation is informational ({@link Level#INFO}) or a
 *       warning ({@link Level#WARN}). Returns {@code this} so callers can inspect
 *       {@link #violations()} after.</li>
 * </ul>
 *
 * <p>Every emission is one increment of a single meter, {@code naturalist.invariant.check},
 * per inspected constraint, tagged {@code outcome=held} or {@code outcome=violated}. The
 * {@link Observer.MonitoringMode} supplied at construction decides which outcomes are
 * emitted: {@link Observer.MonitoringMode#ON_FAILURE} emits the violated checks only;
 * {@link Observer.MonitoringMode#ALWAYS} emits the held checks too — suitable for realtime
 * traffic dashboards. A violation is never counted twice, and an alert on
 * {@code outcome=violated} reads the same in either mode.
 *
 * <p>Every emitted metric also carries a {@code kind} tag — {@code argument}, {@code state} or
 * {@code observable} ({@link ObservationKind}) — naming what was observed: values the caller
 * passed, values the method acquired during its own run, or an observable's declared
 * invariants. Split on {@code kind} to tell a bad caller from a bad dependency.
 *
 * <p>Every emitted metric also carries a {@code level} tag. A violated check takes
 * {@code error} when an exception is being thrown, otherwise the call-site value passed to
 * {@link #observe(Level)}; a held check is always {@code info}. Split on {@code outcome},
 * not {@code level}, to separate held from violated — under {@code observe(Level.INFO)}
 * both carry {@code info}.
 *
 * <p>Metrics are emitted via Micrometer's global {@code Metrics.globalRegistry} —
 * a {@code CompositeMeterRegistry} that delegates to all registered backends.
 * When no backends are configured it is effectively a no-op.
 */
public class InvariantObservation {

    private final String scope;
    private final Set<Constraint<?>> constraints;
    private final Observer.MonitoringMode monitoringMode;
    private final ObservationKind kind;

    InvariantObservation(String scope, Set<Constraint<?>> constraints, Observer.MonitoringMode monitoringMode,
                         ObservationKind kind) {
        this.scope = scope;
        this.constraints = constraints == null ? Set.of() : Set.copyOf(constraints);
        this.monitoringMode = monitoringMode == null ? Observer.MonitoringMode.ON_FAILURE : monitoringMode;
        this.kind = kind == null ? ObservationKind.OBSERVABLE : kind;
    }

    /**
     * All constraints in this observation, valid and invalid.
     */
    public Set<Constraint<?>> constraints() {
        return constraints;
    }

    /**
     * Only the constraints that failed validation — invariant violations.
     */
    public Set<Constraint<?>> violations() {
        return constraints.stream()
                .filter(c -> !c.isValid())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> violationNames() {
        return violations().stream()
                .map(Constraint::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> violationNamesRemovingPrefix(String prefix) {
        return violations().stream()
                .map(Constraint::name)
                .map(n -> n.substring(prefix.length()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Emit metrics and throw {@link InvariantViolationException} if any invariant is violated.
     * Violated checks carry {@code level=error} — the throw is the trigger, the level is fixed.
     * With {@link Observer.MonitoringMode#ALWAYS}, the held checks are emitted too, tagged
     * {@code outcome=held} and {@code level=info}.
     */
    public void throwWhenInvalid() {
        Set<Constraint<?>> violated = violations();
        // Violations emit at ERROR — the throw is in flight. A held check is not the cause of
        // any pending throw, so ALWAYS mode tags it INFO.
        emitMetrics(Level.ERROR, Level.INFO);
        if (!violated.isEmpty()) {
            throw new InvariantViolationException(scope, violated);
        }
    }

    /**
     * Emit metrics without throwing. Every violated check carries {@code level} as its
     * {@code level} tag — only the call site knows whether this observation is informational or a
     * warning, so the call site supplies it. Under {@link Observer.MonitoringMode#ALWAYS} the
     * checks that held are emitted too, tagged {@code outcome=held} and {@link Level#INFO}: a held
     * check is baseline signal, never a warning, whatever level the call site chose for the
     * violations — the same split {@link #throwWhenInvalid()} makes between {@code ERROR} and
     * {@code INFO}. Returns {@code this} so callers can inspect the observation after metric
     * emission:
     *
     * <pre>{@code
     * InvariantObservation obs = observer.entity(e, "created").observe(Level.INFO);
     * if (!obs.violations().isEmpty()) { ... }
     * }</pre>
     */
    public InvariantObservation observe(Level level) {
        Level resolved = level == null ? Level.INFO : level;
        emitMetrics(resolved, Level.INFO);
        return this;
    }

    public String scope() {
        return scope;
    }

    /**
     * What this observation looked at — the caller's arguments, the method's acquired state, or
     * an observable's own invariants. Emitted as the {@code kind} tag.
     */
    public ObservationKind kind() {
        return kind;
    }

    private void emitMetrics(Level violationLevel, Level heldLevel) {
        // One increment per inspected constraint, on one meter: ON_FAILURE emits the violated
        // checks only, ALWAYS emits the held checks too. A violation is never counted twice.
        try {
            for (Constraint<?> c : constraints) {
                boolean held = c.isValid();
                if (held && monitoringMode != Observer.MonitoringMode.ALWAYS) {
                    continue;
                }
                Metric.counter("naturalist.invariant.check")
                        .tag("constraint", c.getClass().getSimpleName())
                        .tag("class", c.source())
                        .tag("method", c.methodName())
                        .tag("name", c.name())
                        .tag("outcome", held ? "held" : "violated")
                        .tag("level", (held ? heldLevel : violationLevel).tagValue())
                        .tag("kind", kind.tagValue())
                        .incrementCounter();
            }
        } catch (Exception ignored) {
            // The observability framework must never throw from metric emission.
        }
    }
}
