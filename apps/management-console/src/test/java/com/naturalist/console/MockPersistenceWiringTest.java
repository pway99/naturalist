package com.naturalist.console;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The runtime half of the rdbms-isolation gate (the static half is
 * {@code architecture/RdbmsIsolationComplianceTest}): boots the real application context exactly as
 * every other {@code @SpringBootTest} in this module does, and asserts on the beans that were
 * actually registered.
 *
 * <p>This is the assertion that has teeth. The console reached Postgres through classpath scanning
 * — {@code DomainServiceScan} sweeping for {@code @DomainService}, {@code @MapperScan} sweeping for
 * {@code @Mapper} — so the coupling never appeared in any test's bytecode and no static rule could
 * have caught it. Inspecting the assembled context can, and does.
 *
 * <p>Guards the classpath arrangement declared in this module's POM: the {@code *-repository-rdbms}
 * jars dropped from the test classpath by Surefire, and {@code spring-test-data} pulled in at test
 * scope to supply the doubles. Undo either — restore an rdbms jar to the test classpath, or promote
 * {@code spring-test-data} to compile scope — and the app-level tests re-attach to the standing
 * {@code naturalist_test} database that the {@code *-repository-rdbms} ITs treat as a fixed
 * fixture, where a single committed write breaks a different module on the following build.
 */
@SpringBootTest
class MockPersistenceWiringTest {

    @Autowired
    ApplicationContext context;

    @Test
    void contextRegistersNoRdbmsAdapter() {
        assertThat(Arrays.stream(context.getBeanDefinitionNames())
                .map(name -> context.getType(name))
                .filter(type -> type != null)
                .map(Class::getName)
                .filter(name -> name.endsWith("Rdbms"))
                .toList())
                .as("the *-repository-rdbms jars must be off the test classpath, so the scan finds none")
                .isEmpty();
    }

    @Test
    void contextHasNoDataSource() {
        assertThat(context.getBeanNamesForType(DataSource.class))
                .as("app-level tests must not open a database connection; with no rdbms adapter "
                        + "on the classpath nothing asks for one")
                .isEmpty();
    }

    /**
     * The mirror of {@link #contextRegistersNoRdbmsAdapter}: proves the adapters are absent because
     * the mocks replaced them, not because the scan quietly registered nothing at all — which would
     * satisfy every other assertion here while leaving the app unwired.
     *
     * <p>Counts doubles by class name rather than resolving a repository interface, because those
     * interfaces are package-private in each {@code *-api} module by design (CLAUDE.md's repository
     * rule 5) and are not nameable from the app.
     */
    @Test
    void contextRegistersTheMockDoubles() {
        assertThat(Arrays.stream(context.getBeanDefinitionNames())
                .map(name -> context.getType(name))
                .filter(type -> type != null)
                .map(Class::getName)
                .filter(name -> name.endsWith("RepositoryMock"))
                .toList())
                .as("the test-scoped spring-test-data jar must register the @MockDomainService doubles")
                .isNotEmpty();
    }
}
