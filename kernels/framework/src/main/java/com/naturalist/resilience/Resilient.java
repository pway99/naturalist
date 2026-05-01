package com.naturalist.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a class or method is governed by a named resilience strategy.
 * The {@code name} resolves to the {@link ResilienceConfig} entries registered
 * under that name in the active {@link Resilience} adapter; an adapter applies
 * every primitive configured under that name when wrapping the call.
 *
 * <p>This is a pure marker — no Spring or other framework meta-annotation. The
 * adapter that bridges this annotation to runtime behaviour lives in
 * {@code adapters/spring-runtime/} (or its successor); domain code never
 * imports the bridge.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Resilient {

    String name();
}
