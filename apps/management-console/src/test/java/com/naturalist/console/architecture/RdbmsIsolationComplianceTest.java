package com.naturalist.console.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The console's tests run on in-memory {@code @MockDomainService} doubles and must never reach the
 * rdbms stack — no repository adapter, no mapper, no {@code DataSource}.
 *
 * <h2>Why this is the static half of a two-part gate</h2>
 * These rules are necessary but <strong>not sufficient</strong>, and it matters that the next reader
 * knows why. The console's coupling to Postgres was never expressed in bytecode: no test class ever
 * named an rdbms type. The adapters arrived at runtime, through
 * {@code DomainServiceScan}'s classpath sweep for {@code @DomainService} and {@code @MapperScan}'s
 * sweep for {@code @Mapper}. A dependency rule alone would have passed cleanly through the entire
 * period when every one of these tests was talking to the standing {@code naturalist_test} database
 * — and one of them was corrupting it.
 *
 * <p>So the runtime half lives in {@code MockPersistenceWiringTest}, which boots the context and
 * asserts on the beans that actually got registered. That test is what has teeth. These rules stop
 * the regression the scan cannot see: a test reaching for an adapter, a mapper or a
 * {@code DataSource} by hand, which would sidestep the profile switch entirely.
 *
 * <p>Scoped to test classes via {@link ImportOption.OnlyIncludeTests}, because the app's main code
 * legitimately depends on all of this — {@code RdbmsPersistenceConfiguration} exists precisely to
 * wire it for production.
 */
@AnalyzeClasses(packages = "com.naturalist.console", importOptions = ImportOption.OnlyIncludeTests.class)
class RdbmsIsolationComplianceTest {

    @ArchTest
    static final ArchRule testsDoNotTouchRdbmsAdapters =
            noClasses()
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Rdbms")
                    .orShould().dependOnClassesThat().resideInAPackage("com.naturalist.persistence..")
                    .because("console tests run on @MockDomainService doubles, the rdbms jars "
                            + "being off the test classpath; naming an adapter reintroduces the standing "
                            + "naturalist_test Postgres that the *-repository-rdbms ITs own")
                    .allowEmptyShould(true);

    /**
     * The runtime gate is the single exemption: {@code MockPersistenceWiringTest} names
     * {@code DataSource} in order to assert the context contains no bean of that type. It is the
     * one place a reference proves absence rather than use.
     */
    private static final String RUNTIME_GATE = "com.naturalist.console.MockPersistenceWiringTest";

    @ArchTest
    static final ArchRule testsDoNotTouchDataSources =
            noClasses()
                    .that().doNotHaveFullyQualifiedName(RUNTIME_GATE)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "javax.sql..", "com.zaxxer.hikari..", "org.apache.ibatis..", "org.mybatis..")
                    .because("no app-level test may open a database connection or build a mapper; "
                            + "this context has no DataSource at all")
                    .allowEmptyShould(true);
}
