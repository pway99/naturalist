package com.naturalist.exception;

import com.naturalist.observability.Constraint;

import java.util.Collection;
import java.util.List;

/**
 * Thrown when one or more invariants are violated during argument validation or entity
 * state observation. Carries the complete set of violations so the caller sees every
 * failure in a single pass — no iterative error discovery.
 *
 * <p>{@link #getMessage()} serialises scope and all violation names into a single readable
 * string. {@link #violations()} exposes the raw {@link Constraint} instances for programmatic
 * inspection.
 */
public class InvariantViolationException extends RuntimeException {

    private final String scope;
    private final List<Constraint<?>> violations;

    public InvariantViolationException(String scope, Collection<? extends Constraint<?>> violations) {
        super(buildMessage(scope, violations));
        this.scope = scope;
        this.violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public String scope() {
        return scope;
    }

    public List<Constraint<?>> violations() {
        return violations;
    }

    private static String buildMessage(String scope, Collection<? extends Constraint<?>> violations) {
        if (violations == null || violations.isEmpty()) {
            return scope + ": invariant violation";
        }
        StringBuilder sb = new StringBuilder(scope)
                .append(": ")
                .append(violations.size())
                .append(" invariant violation(s)");
        for (Constraint<?> v : violations) {
            sb.append("\n  - ").append(v.name());
        }
        return sb.toString();
    }
}