package com.naturalist.exception;

/**
 * Thrown when domain code asks a {@code Resilience} adapter for a primitive
 * keyed by a name that the composition root never registered. The adapter
 * refuses to serve an unprotected fall-back: an unconfigured strategy is a
 * deployment defect, not a runtime condition to absorb silently.
 */
public class UnconfiguredResilienceException extends RuntimeException {

    private final String primitive;
    private final String name;

    public UnconfiguredResilienceException(String primitive, String name) {
        super("No %s configured for resilience strategy '%s'".formatted(primitive, name));
        this.primitive = primitive;
        this.name = name;
    }

    public String primitive() {
        return primitive;
    }

    public String name() {
        return name;
    }
}
