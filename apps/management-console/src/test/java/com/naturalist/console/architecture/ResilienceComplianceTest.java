package com.naturalist.console.architecture;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.EntityReferences;
import com.naturalist.resilience.Resilience;
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

import java.util.List;

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
 *   <li>HTTP clients — any class touching a known HTTP transport package. The
 *       widest rule and the one that catches external-service adapters, which
 *       none of the structural rules below reach.</li>
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
 * <p>A fifth rule runs the other direction — every {@code @Resilient}
 * declaration must reach the {@code Resilience} facade — catching the
 * declaration that reads as protection but compiles to nothing. See
 * {@link #resilientDeclarationsReachTheFacade}.
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

    /**
     * The widest of the discovery rules, and the one that would have caught
     * {@code AnthropicVisionService} on the day it was written rather than
     * relying on a reviewer noticing. An HTTP call leaves the process by
     * definition; the {@code Catalog}/{@code EntityReferences}/{@code rdms}
     * rules are all narrower structural proxies for the same property.
     *
     * <h2>Why an explicit list</h2>
     * "Depends on an HTTP client" is not expressible structurally — there is no
     * common supertype across {@code okhttp3}, {@code java.net.http}, Apache
     * HttpClient, Spring's {@code RestClient}, and every vendor SDK that wraps
     * one. {@link #HTTP_CLIENT_PREFIXES} is therefore a curated list of known
     * entry points, extended when a new client arrives on the classpath — the
     * same trade the {@code ProcessBuilder} rule makes. It is a floor, not a
     * proof: a client nobody listed still slips through, which is why reviewer
     * discipline remains the primary control.
     *
     * <p>Vendor SDKs are listed by their transport package rather than their
     * facade type ({@code com.anthropic.client.okhttp}, not
     * {@code AnthropicClient}), because the transport package is what a class
     * must touch to construct a client, and it is stable across SDK versions.
     */
    @ArchTest
    static final ArchRule httpClientsDeclareResilience =
            classes()
                    .that(dependOnAnHttpClient())
                    .and().areNotInterfaces()
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .should(declareResilience())
                    .because("an HTTP call leaves the process and can hang on a "
                            + "network no caller controls — see docs/resilience-policy.md");

    @ArchTest
    static final ArchRule subprocessSpawnersDeclareResilience =
            classes()
                    .that(dependOnClass("java.lang.ProcessBuilder"))
                    .and().areNotInterfaces()
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .should(declareResilience())
                    .because("spawning an OS subprocess is a cross-boundary call — "
                            + "see docs/resilience-policy.md");

    /**
     * The inverse of the four rules above. Those ask "is a cross-boundary class
     * missing a declaration?"; this asks "is a declaration missing its code?"
     *
     * <p>Nothing reads {@code @Resilient} at runtime — no weaver, no proxy. A
     * class that declares it and never touches {@link Resilience} is protected
     * by nothing at all, while reading to the next developer as protected. That
     * is strictly worse than an honest omission, and it is what
     * {@code AnthropicVisionService} was: annotated
     * {@code @Resilient(name = "vision.identification")} with no facade call
     * and no registered config, so every vision identification ran unbounded.
     *
     * <h2>The {@code EntityReferences} carve-out</h2>
     * A provider is wrapped <em>by its caller</em>:
     * {@code InMemoryCatalog.findReferencesTo} applies {@code catalog.fanout}'s
     * timeout and breaker around each provider invocation individually, so that
     * one wedged domain degrades only its own slice of the response. The
     * providers' own {@code @Resilient(name = "catalog.fanout")} declares
     * coverage, not application, and they correctly inject nothing. Excluding
     * them is a statement about that call shape, not an exemption list — a new
     * provider inherits the carve-out automatically, and any other class that
     * wants coverage-without-application has to justify a second one.
     */
    @ArchTest
    static final ArchRule resilientDeclarationsReachTheFacade =
            classes()
                    .that(declareResilient())
                    .and().areNotInterfaces()
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .and(DescribedPredicate.not(JavaClass.Predicates.implement(EntityReferences.class)))
                    .should(useTheResilienceFacade())
                    .because("a @Resilient declaration that never reaches the facade "
                            + "protects nothing — see docs/resilience-policy.md");

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

    /**
     * Known HTTP transport entry points, by package prefix. Extend when a new
     * client lands on the classpath; a missing entry weakens the rule silently,
     * so a PR introducing an HTTP client should add its prefix here in the same
     * change.
     */
    private static final List<String> HTTP_CLIENT_PREFIXES = List.of(
            "okhttp3",
            "com.anthropic.client.okhttp",
            "java.net.http",
            "org.apache.hc.client5.http",
            "org.apache.http.client",
            "org.springframework.web.client",
            "org.springframework.web.reactive.function.client");

    /** Individually named HTTP types that no package prefix cleanly covers. */
    private static final List<String> HTTP_CLIENT_CLASSES = List.of(
            "java.net.HttpURLConnection",
            "java.net.URLConnection");

    private static DescribedPredicate<JavaClass> dependOnAnHttpClient() {
        return new DescribedPredicate<>("depend on an HTTP client") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getDirectDependenciesFromSelf().stream()
                        .map(d -> d.getTargetClass().getFullName())
                        .anyMatch(ResilienceComplianceTest::isHttpClientType);
            }
        };
    }

    private static boolean isHttpClientType(String fullyQualifiedName) {
        return HTTP_CLIENT_CLASSES.contains(fullyQualifiedName)
                || HTTP_CLIENT_PREFIXES.stream()
                .anyMatch(prefix -> fullyQualifiedName.startsWith(prefix + "."));
    }

    private static DescribedPredicate<JavaClass> dependOnClass(String fullyQualifiedName) {
        return new DescribedPredicate<>("depend on " + fullyQualifiedName) {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getDirectDependenciesFromSelf().stream()
                        .anyMatch(d -> d.getTargetClass().getFullName().equals(fullyQualifiedName));
            }
        };
    }

    private static DescribedPredicate<JavaClass> declareResilient() {
        return new DescribedPredicate<>("declare @Resilient") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.isAnnotatedWith(Resilient.class)
                        || javaClass.getMethods().stream()
                        .anyMatch(m -> m.isAnnotatedWith(Resilient.class));
            }
        };
    }

    private static ArchCondition<JavaClass> useTheResilienceFacade() {
        return new ArchCondition<>("depend on the Resilience facade") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                boolean reachesFacade = clazz.getDirectDependenciesFromSelf().stream()
                        .anyMatch(d -> d.getTargetClass().getFullName().equals(Resilience.class.getName()));
                if (!reachesFacade) {
                    events.add(SimpleConditionEvent.violated(clazz, String.format(
                            "%s declares @Resilient but never depends on %s — nothing reads "
                                    + "the annotation at runtime, so the call site is unprotected. "
                                    + "Wrap the call through the facade, or drop the declaration "
                                    + "for @ResilienceExempt with a reason. "
                                    + "See docs/resilience-policy.md",
                            clazz.getName(), Resilience.class.getName())));
                }
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
