package com.naturalist.console.architecture;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.EntityReferences;
import com.naturalist.resilience.ResilienceExempt;
import com.naturalist.resilience.Resilient;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Build-time enforcement of the {@code docs/resilience-policy.md} contract:
 * every concrete class on a known cross-boundary call path must declare its
 * resilience strategy via {@link Resilient} (programmatic facade use counts —
 * the annotation marks intent), or carry a {@link ResilienceExempt} with a
 * documented reason. Reviewer discipline catches the rest; this test is the
 * safety net for missed sites.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@link Catalog} implementations — they fan out to every registered
 *       provider; a hung provider must be time-bounded.</li>
 *   <li>{@link EntityReferences} implementations — they are invoked from the
 *       fan-out and so are themselves a cross-boundary call site whose
 *       providers may eventually be repository-backed.</li>
 *   <li>Subprocess spawners — any class depending on {@link ProcessBuilder}
 *       lives at a clear OS-process boundary; the {@code sips} call inside
 *       {@code InsectsController.image} is the only current example.</li>
 *   <li>RDMS repository adapters — any class in a package whose name
 *       includes {@code rdms} is presumed to cross a database boundary. The
 *       soil module's {@code soil-repository-rdms} is the named candidate;
 *       no class ships there yet, so the rule passes vacuously today and
 *       arms the gate for the first such adapter.</li>
 * </ul>
 *
 * <h2>Scope</h2>
 * Test classes are excluded via {@link ImportOption.DoNotIncludeTests} —
 * synthetic providers in {@code CatalogResilienceTest} and similar fixtures
 * are not subject to the policy.
 */
@AnalyzeClasses(
        packages = "com.naturalist",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ResilienceComplianceTest {

    @ArchTest
    static final ArchRule catalogImplementationsDeclareResilience =
            classes()
                    .that().implement(Catalog.class)
                    .and().areNotInterfaces()
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .should(declareResilience())
                    .because("a Catalog fan-out is a cross-boundary call site — "
                            + "see docs/resilience-policy.md");

    @ArchTest
    static final ArchRule entityReferencesProvidersDeclareResilience =
            classes()
                    .that().implement(EntityReferences.class)
                    .and().areNotInterfaces()
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .should(declareResilience())
                    .because("an EntityReferences provider is invoked from "
                            + "Catalog fan-out — see docs/resilience-policy.md");

    @ArchTest
    static final ArchRule subprocessSpawnersDeclareResilience =
            classes()
                    .that(dependOnClass("java.lang.ProcessBuilder"))
                    .and().areNotInterfaces()
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .should(declareResilience())
                    .because("spawning an OS subprocess is a cross-boundary call — "
                            + "see docs/resilience-policy.md");

    @ArchTest
    static final ArchRule rdmsAdaptersDeclareResilience =
            classes()
                    .that().resideInAPackage("..rdms..")
                    .and().areNotInterfaces()
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .should(declareResilience())
                    .because("an RDMS repository adapter crosses a database boundary — "
                            + "see docs/resilience-policy.md")
                    .allowEmptyShould(true);

    private static DescribedPredicate<JavaClass> dependOnClass(String fullyQualifiedName) {
        return new DescribedPredicate<>("depend on " + fullyQualifiedName) {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getDirectDependenciesFromSelf().stream()
                        .anyMatch(d -> d.getTargetClass().getFullName().equals(fullyQualifiedName));
            }
        };
    }

    private static ArchCondition<JavaClass> declareResilience() {
        return new ArchCondition<>("declare a resilience strategy via @Resilient or @ResilienceExempt") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                boolean classLevel = clazz.isAnnotatedWith(Resilient.class)
                        || clazz.isAnnotatedWith(ResilienceExempt.class);
                boolean methodLevel = clazz.getMethods().stream().anyMatch(m ->
                        m.isAnnotatedWith(Resilient.class)
                                || m.isAnnotatedWith(ResilienceExempt.class));
                if (!classLevel && !methodLevel) {
                    events.add(SimpleConditionEvent.violated(clazz, String.format(
                            "%s is on a cross-boundary call path but declares no "
                                    + "@Resilient or @ResilienceExempt — see "
                                    + "docs/resilience-policy.md",
                            clazz.getName())));
                }
            }
        };
    }
}
