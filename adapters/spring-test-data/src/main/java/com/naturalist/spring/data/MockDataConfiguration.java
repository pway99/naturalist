package com.naturalist.spring.data;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Registers in-memory persistence for a whole application context: every
 * {@link com.naturalist.data.MockDomainService @MockDomainService} double becomes a bean, standing
 * in for the {@code *-repository-rdbms} adapter that would otherwise serve the same repository
 * interface.
 *
 * <h2>The classpath is the switch — there is no flag</h2>
 * This class carries no {@code @Profile} and reads no property. It is picked up by the app's
 * component scan whenever it is present, and it is present exactly when a module depends on
 * {@code spring-test-data} — which apps do at {@code <scope>test</scope>}. In a packaged
 * deployment the jar is absent, this class does not exist, and no mock can register even by
 * mistake. That is the same discipline {@code TestDataConfiguration} has always followed, and it
 * beats a profile check: a profile that is merely forgotten silently boots a test against
 * production persistence, whereas a jar that is absent cannot be forgotten into existence.
 *
 * <p>The mirror half is the app's Surefire {@code classpathDependencyExcludes}, which drops the
 * {@code *-repository-rdbms} jars from the test classpath so their {@code @DomainService} adapters
 * are not there to be scanned. Both halves are classpath facts, stated in the app's POM.
 *
 * <h2>Why app-level tests want this</h2>
 * A {@code @SpringBootTest} that boots the real adapters talks to the standing {@code naturalist_test}
 * Postgres, which the {@code *-repository-rdbms} ITs treat as a fixed fixture. Any write such a test
 * performs commits and corrupts that fixture — silently, and for the <em>next</em> build rather than
 * its own. Wiring the mocks removes the hazard at its source and gives the app module a
 * database-free {@code mvn test}.
 */
@Configuration
@Import(MockDomainServiceScan.class)
public class MockDataConfiguration {
}
