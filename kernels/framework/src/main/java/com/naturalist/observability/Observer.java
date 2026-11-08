package com.naturalist.observability;

import com.naturalist.ddd.Named;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The single entry point into the observability framework. An {@code Observer} performs
 * four complementary roles over the same constraint-graph walk:
 *
 * <ol>
 *   <li><b>Argument validation.</b> {@link #arguments(String, Consumer)} builds an
 *       {@link InvariantObservation} over the constraints declared for a method's arguments —
 *       external input crossing the method boundary. Calling
 *       {@link InvariantObservation#throwWhenInvalid()} throws
 *       {@link com.naturalist.exception.InvariantViolationException} carrying every violation
 *       in a single pass.</li>
 *   <li><b>Method-body observation.</b> {@link #namedEntity(Named, String)} and
 *       {@link #observable(Observable, String)} produce an {@link InvariantObservation} for
 *       state the method has constructed or received — the method's own results, not
 *       external input. The terminal operation follows the producer/consumer rule
 *       (ADR-017): a producer observing its own output emits metrics via
 *       {@link InvariantObservation#observe()} and returns the value so the consumer
 *       retains control over the data flow. Only a consumer observing a value it received
 *       may choose {@link InvariantObservation#throwWhenInvalid()}. Argument validation
 *       always throws — it is the producer's boundary contract.</li>
 *   <li><b>State validation.</b> {@link #state(String, Consumer)} builds the same kind of
 *       observation over values the method acquired during its own run — an aggregate fetched
 *       from another api, a count, a looked-up value. Arguments are what the caller handed in;
 *       state is what the method obtained. Emissions are tagged {@code kind=state} (arguments
 *       {@code kind=argument}, observables {@code kind=observable}) on the one
 *       {@code naturalist.invariant.check} meter. The terminal operation is the consumer's
 *       choice.</li>
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
        // A synthetic class, one a tool generated (a test graph's traced subclass), is named for the class it extends,
        // so its metrics read as the production class's do.
        Class<?> named = referenceClass;
        while (named.isSynthetic() && named.getSuperclass() != null) {
            named = named.getSuperclass();
        }
        this.referenceClass = named;
        this.monitoringMode = monitoringMode == null ? MonitoringMode.ON_FAILURE : monitoringMode;
    }

    /**
     * Construct an observer in {@link MonitoringMode#ON_FAILURE} mode.
     */
    public static Observer forClass(Class<?> referenceClass) {
        return new Observer(referenceClass, MonitoringMode.ON_FAILURE);
    }

    /**
     * Construct an observer with an explicit monitoring mode.
     */
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
     * For values the method acquires during its own run, use {@link #state(String, Consumer)}.
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
        return constraintBatch(methodName, constraintConsumer, ObservationKind.ARGUMENT);
    }

    // ---------------------------------------------------------------------------------
    // State validation — values the method acquired during its own run
    // ---------------------------------------------------------------------------------

    /**
     * Build an {@link InvariantObservation} over the state a method body depends on — values it
     * acquired or computed during its own run rather than received from its caller: an entity or
     * aggregate fetched from another api, a count, a looked-up mapping, a string read off a
     * result. The constraint declarations and scope ({@code ClassName.methodName.label}) are the
     * same as {@link #arguments(String, Consumer)}; only the meaning differs, and every emission
     * carries {@code kind=state} instead of {@code kind=argument} so a violation points at what
     * the method consumed, not at its caller.
     *
     * <p>Unlike arguments, the terminal operation is the call site's choice (ADR-017): a consumer
     * that cannot continue on invalid state calls {@link InvariantObservation#throwWhenInvalid()};
     * one that can degrade calls {@link InvariantObservation#observe(Level)}.
     *
     * <pre>{@code
     * Zone zone = zoneQuery.getByName(zoneName);
     * observer.state("assignPlot", s -> s
     *         .notNull(zone, "zone")
     *         .atLeast(zone.plotCount(), 1, "plotCount")
     *         .notBlank(zone.ownerHandle(), "ownerHandle"))
     *     .throwWhenInvalid();
     * }</pre>
     *
     * <p>To walk an acquired {@link Observable}'s own declared invariants rather than the facts
     * this method depends on, use {@link #observable(Observable, String)}.
     */
    public InvariantObservation state(String methodName, Consumer<? extends Constraints> constraintConsumer) {
        return constraintBatch(methodName, constraintConsumer, ObservationKind.STATE);
    }

    private InvariantObservation constraintBatch(String methodName, Consumer<? extends Constraints> constraintConsumer,
                                                 ObservationKind kind) {
        Constraints accumulator = new Constraints();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Consumer<Constraints> consumer = (Consumer) constraintConsumer;
        consumer.accept(accumulator);

        String scope = referenceClass.getSimpleName() + "." + methodName;
        return new InvariantObservation(scope, flatten(accumulator, scope + "."), monitoringMode, kind);
    }

    // ---------------------------------------------------------------------------------
    // Method-body observation — the method's own results, not external input
    // ---------------------------------------------------------------------------------

    /**
     * Observe a named entity within a method body. Walks the entity's full constraint
     * graph. Returns an {@link InvariantObservation} the caller can inspect.
     *
     * <p>Terminal-operation choice follows the producer/consumer rule (ADR-017):
     * producers emitting their own output call {@link InvariantObservation#observe()};
     * only consumers acting on received state may call
     * {@link InvariantObservation#throwWhenInvalid()}.
     */
    public InvariantObservation namedEntity(Named<?> entity, String label) {
        return observable(entity, label);
    }

    /**
     * Observe any named {@link Observable} within a method body. Walks the observable's
     * full constraint graph. A {@code null} observable produces a single failing
     * {@code notNull} constraint. To check specific facts this method depends on rather than
     * the observable's own invariants, use {@link #state(String, Consumer)}.
     */
    public InvariantObservation observable(Observable observable, String label) {
        String scope = referenceClass.getSimpleName() + "." + label;
        if (observable == null) {
            Constraints accumulator = new Constraints();
            accumulator.notNull(this, ignored -> null, label);
            return new InvariantObservation(scope, flatten(accumulator, scope + "."), monitoringMode, ObservationKind.OBSERVABLE);
        }
        Constraints accumulator = new Constraints();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Consumer<Constraints> consumer = (Consumer) observable.invariants();
        consumer.accept(accumulator);
        return new InvariantObservation(scope, flatten(accumulator, scope + "."), monitoringMode, ObservationKind.OBSERVABLE);
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
            return new InvariantObservation(scope, flatten(accumulator, scope + "."), monitoringMode, ObservationKind.OBSERVABLE);
        }
        Constraints accumulator = new Constraints();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Consumer<Constraints> consumer = (Consumer) observable.invariants();
        consumer.accept(accumulator);

        String scope = referenceClass.getSimpleName() + "." + observable.getClass().getSimpleName();
        return new InvariantObservation(scope, flatten(accumulator, scope + "."), monitoringMode, ObservationKind.OBSERVABLE);
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
