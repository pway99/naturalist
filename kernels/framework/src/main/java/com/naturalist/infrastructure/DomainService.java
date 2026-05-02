package com.naturalist.infrastructure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for a class that participates in the runtime composition of a domain
 * — a contribution, an inverse provider, a domain identity, a query
 * implementation, or any collaborator a runtime adapter is expected to
 * register and instantiate.
 *
 * <p>The annotation carries no third-party meta-annotations. Domain
 * {@code *-core} modules import this marker only; they do not import Spring,
 * Guice, Helidon, or any other DI framework. A runtime adapter
 * (see {@code adapters/spring-runtime/}) discovers annotated classes via
 * classpath scanning and registers them with its container.
 *
 * <p>Retention is {@code RUNTIME} and target is {@code TYPE} so reflection-
 * based scanners can locate the marker on compiled classes at startup.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DomainService {
}
