package com.naturalist.observability;

import com.naturalist.ddd.Entity;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The single entry point into the observability framework. An {@code Observer} performs
 * three complementary roles over the same constraint-graph walk:
 *
 * <ol>
 *   <li><b>Argument validation.</b> {@link #arguments(String, Consumer)} builds an
 *       {@link InvariantObservation} over the constraints declared for a method's arguments —
 *       external input crossing the method boundary. Calling
 *       {@link InvariantObservation#throwWhenInvalid()} throws
 *       {@link com.naturalist.exception.InvariantViolationException} carrying every violation
 *       in a single pass.</li>
 *   <li><b>Method-body observation.</b> {@link #entity(Entity, String)} and
 *       {@link #observable(Observable, String)} produce an {@link InvariantObservation} for
 *       state the method has constructed or received — the method's own results, not
 *       external input. The caller chooses {@link InvariantObservation#throwWhenInvalid()}
 *       or {@link InvariantObservation#observe()} (metrics only, no throw).</li>
 *   <li><b>Realtime monitoring.</b> Constructing an observer with
 *       {@link MonitoringMode#ALWAYS} causes every observation to emit a metric for every
 *       constraint inspected, valid or invalid. With {@link MonitoringMode#ON_FAILURE} (the
 *       default) a metric is emitted only per violation.</li>
 * </ol>
 *
 * <p>Metrics are emitted via Micrometer's global {@code Metrics.globalRegistry} —
 * a {@code CompositeMeterRegistry} resolved at emission time, not at Observer
 * construction time. When no backends are registered it is effectively a no-op.
 */
public class Observer {

    /**
     * Controls how {@link InvariantObservation} emits metrics.
     */
    public enum MonitoringMode {
        /**
         * Emit a metric only for each constraint that fails. The default — low cardinality,
         * used for resilience signalling.
         */
        ON_FAILURE,

        /**
         * Emit a metric for every constraint inspected, valid or invalid. Higher cardinality;
         * used for realtime monitoring of state flowing through the system.
         */
        ALWAYS
    }

    private final Class<?> referenceClass;
    private final MonitoringMode monitoringMode;

    private Observer(Class<?> referenceClass, MonitoringMode monitoringMode) {
        if (referenceClass == null) {
            throw new IllegalArgumentException("referenceClass must not be null");
        }
        this.referenceClass = referenceClass;
        this.monitoringMode = monitoringMode == null ? MonitoringMode.ON_FAILURE : monitoringMode;
    }

    /** Construct an observer in {@link MonitoringMode#ON_FAILURE} mode. */
    public static Observer forClass(Class<?> referenceClass) {
        return new Observer(referenceClass, MonitoringMode.ON_FAILURE);
    }

    /** Construct an observer with an explicit monitoring mode. */
    public static Observer forClass(Class<?> referenceClass, MonitoringMode monitoringMode) {
        return new Observer(referenceClass, monitoringMode);
    }

    // ---------------------------------------------------------------------------------
    // Method-scoped observer
    // ---------------------------------------------------------------------------------

    /**
     * Return a {@link MethodObserver} scoped to the given method name. All observations
     * produced by the returned observer carry scope {@code ClassName.methodName.label}.
     */
    public MethodObserver forMethod(String methodName) {
        return new MethodObserver(referenceClass, methodName, monitoringMode);
    }

    // ---------------------------------------------------------------------------------
    // Argument validation — external input crossing the method boundary
    // ---------------------------------------------------------------------------------

    /**
     * Build an {@link InvariantObservation} over a method's argument constraints.
     * Arguments are external input — user-supplied values crossing the method boundary.
     * The returned observation is terminal: the caller invokes
     * {@link InvariantObservation#throwWhenInvalid()} to validate, or inspects
     * {@link InvariantObservation#violations()} directly.
     *
     * <pre>{@code
     * observer.arguments("getByName", i -> i.entityName(name, "name"))
     *         .throwWhenInvalid();
     *
     * observer.arguments("doSomething", i -> i
     *         .notNull(entity, "entity")
     *         .notNull(vo, "vo")
     * ).throwWhenInvalid();
     * }</pre>
     */
    public InvariantObservation arguments(String methodName, Consumer<? extends Constraints> constraintConsumer) {
        Constraints accumulator = new Constraints();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Consumer<Constraints> consumer = (Consumer) constraintConsumer;
        consumer.accept(accumulator);

        String scope = referenceClass.getSimpleName() + "." + methodName;
        return new InvariantObservation(scope, flatten(accumulator, scope + "."), monitoringMode);
    }

    // ---------------------------------------------------------------------------------
    // Method-body observation — the method's own results, not external input
    // ---------------------------------------------------------------------------------

    /**
     * Observe a named entity within a method body. Walks the entity's full constraint
     * graph. Returns an {@link InvariantObservation} the caller can inspect or throw from.
     *
     * <pre>{@code
     * Entity created = ...;
     * observer.entity(created, "created").throwWhenInvalid();
     * observer.entity(created, "created").observe(); // metrics only
     * }</pre>
     */
    public InvariantObservation entity(Entity<?, ?> entity, String label) {
        return observable(entity, label);
    }

    /**
     * Observe any named {@link Observable} within a method body. Walks the observable's
     * full constraint graph. A {@code null} observable produces a single failing
     * {@code notNull} constraint.
     */
    public InvariantObservation observable(Observable observable, String label) {
        String scope = referenceClass.getSimpleName() + "." + label;
        if (observable == null) {
            Constraints accumulator = new Constraints();
            accumulator.notNull(this, ignored -> null, label);
            return new InvariantObservation(scope, flatten(accumulator, scope + "."), monitoringMode);
        }
        Constraints accumulator = new Constraints();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Consumer<Constraints> consumer = (Consumer) observable.invariants();
        consumer.accept(accumulator);
        return new InvariantObservation(scope, flatten(accumulator, scope + "."), monitoringMode);
    }

    // ---------------------------------------------------------------------------------
    // Unlabelled observation
    // ---------------------------------------------------------------------------------

    /**
     * Walks an {@link Observable}'s constraints into a flattened observation without
     * throwing. Scope is derived from the observable's class name.
     */
    public InvariantObservation observation(Observable observable) {
        if (observable == null) {
            String scope = referenceClass.getSimpleName() + ".<null>";
            Constraints accumulator = new Constraints();
            accumulator.notNull(this, ignored -> null, "observable");
            return new InvariantObservation(scope, flatten(accumulator, scope + "."), monitoringMode);
        }
        Constraints accumulator = new Constraints();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Consumer<Constraints> consumer = (Consumer) observable.invariants();
        consumer.accept(accumulator);

        String scope = referenceClass.getSimpleName() + "." + observable.getClass().getSimpleName();
        return new InvariantObservation(scope, flatten(accumulator, scope + "."), monitoringMode);
    }

    /**
     * Walks the observable's constraint graph and returns a flattened set where each
     * constraint is renamed to its fully-qualified dotted path rooted at
     * {@code referenceClass.getSimpleName()}.
     * <p>
     * Does not throw or filter — callers inspect {@link Constraint#isValid()} on the
     * returned set for validation, metrics, or observation purposes.
     */
    public Set<Constraint<?>> flattenedConstraints(Observable observable) {
        if (observable == null) {
            return Set.of();
        }
        Constraints accumulator = new Constraints();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Consumer<Constraints> consumer = (Consumer) observable.invariants();
        consumer.accept(accumulator);
        return flatten(accumulator, referenceClass.getSimpleName() + ".");
    }

    // ---------------------------------------------------------------------------------
    // Shared constraint-graph walker
    // ---------------------------------------------------------------------------------

    private Set<Constraint<?>> flatten(Constraints accumulator, String prefix) {
        Set<Constraint<?>> result = new LinkedHashSet<>();
        for (Constraint<?> constraint : accumulator.collected()) {
            result.add(constraint.withName(prefix + constraint.name()));
            if (constraint instanceof ConstraintCollection nested) {
                for (Constraint<?> child : nested.collectConstraints()) {
                    result.add(child.withName(prefix + child.name()));
                }
            }
        }
        return result;
    }
}