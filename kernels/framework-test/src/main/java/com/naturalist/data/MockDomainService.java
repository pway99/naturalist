package com.naturalist.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for an in-memory test double that stands in for a
 * {@link com.naturalist.infrastructure.DomainService @DomainService} adapter — typically a
 * {@code <Entity>RepositoryMock} backed by the shared {@link NaturalistDatabase} JSON graph.
 *
 * <p>This is the test-side twin of {@code @DomainService}, and it carries no third-party
 * meta-annotations for the same reason: a {@code <domain>-repository-test} module must not
 * import Spring. A runtime adapter discovers the marker by classpath scan and registers the
 * annotated classes with its container — see {@code MockDomainServiceScan} in
 * {@code adapters/spring-test-data}.
 *
 * <h2>Why a separate marker rather than {@code @DomainService}</h2>
 * The two markers partition the same classpath into a production half and a test half. An app
 * that wants mock persistence activates the mock scan <em>and</em> excludes the real adapters
 * from the production scan; because the markers are distinct, neither scan has to reason about
 * the other's members, and a mock can never be registered by accident in a production context —
 * the production registrar simply does not look for this annotation.
 *
 * <p>Annotated classes may be package-private, as the mocks are; the scanner instantiates them
 * reflectively and callers inject by interface type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MockDomainService {
}
