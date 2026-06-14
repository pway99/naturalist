package com.naturalist.observability;

import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.Named;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A method-scoped observer that qualifies every observation with the owning
 * class and method name. Produced by {@link Observer#forMethod(String)}.
 *
 * <p>Scope format: {@code ClassName.methodName.label} — three segments that locate
 * the observation in class, method, and variable. This is the recommended entry point
 * for unit tests, where the method name disambiguates observations within the same
 * test class:
 *
 * <pre>{@code
 * private static final Observer observer = Observer.forClass(CompoundTest.class);
 *
 * @Test void compoundIsValid() {
 *     var mo = observer.forMethod("compoundIsValid");
 *     Compound c = ...;
 *     InvariantObservation result = mo.observable(c, "c");
 *     assertThat(result.violations()).isEmpty();
 * }
 * }</pre>
 */
public class MethodObserver {

    private final Class<?> referenceClass;
    private final String methodName;
    private final Observer.MonitoringMode monitoringMode;

    MethodObserver(Class<?> referenceClass, String methodName, Observer.MonitoringMode monitoringMode) {
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("methodName must not be null or blank");
        }
        this.referenceClass = referenceClass;
        this.methodName = methodName;
        this.monitoringMode = monitoringMode;
    }

    public String observationPoint() {
        return referenceClass.getSimpleName() + "." + methodName;
    }

    /**
     * Observe an {@link EntityName} within this method scope. Validates the name's
     * kebab-case format and length constraints. Returns an {@link InvariantObservation}
     * the caller can inspect or observe.
     */
    public <E extends EntityName> InvariantObservation entityName(E name, String label) {
        String scope = referenceClass.getSimpleName() + "." + methodName + "." + label;
        Constraints accumulator = new Constraints();
        accumulator.entityName(name, label);
        return new InvariantObservation(scope, flatten(accumulator, scope + "."), monitoringMode);
    }

    /**
     * Observe a named entity within this method scope.
     */
    public InvariantObservation namedEntity(Named<?> entity, String label) {
        return observable(entity, label);
    }

    /**
     * Observe any named {@link Observable} within this method scope. A {@code null}
     * observable produces a single failing {@code notNull} constraint.
     */
    public InvariantObservation observable(Observable observable, String label) {
        String scope = referenceClass.getSimpleName() + "." + methodName + "." + label;
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

    /**
     * Build an {@link InvariantObservation} over argument constraints within this method
     * scope. The method name is already captured — supply only the constraint declarations.
     */
    public InvariantObservation arguments(Consumer<? extends Constraints> constraintConsumer) {
        Constraints accumulator = new Constraints();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Consumer<Constraints> consumer = (Consumer) constraintConsumer;
        consumer.accept(accumulator);

        String scope = referenceClass.getSimpleName() + "." + methodName;
        return new InvariantObservation(scope, flatten(accumulator, scope + "."), monitoringMode);
    }

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
