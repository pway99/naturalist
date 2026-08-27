package com.naturalist.console.resilience;

import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Resilient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Startup gate: every {@code @Resilient(name = ...)} declared by a registered
 * bean must resolve to a {@link com.naturalist.resilience.ResilienceConfig}
 * the composition root registered under that name.
 *
 * <h2>The defect this closes</h2>
 * {@code @Resilient} is a pure marker — no weaver reads it. A class can carry
 * {@code @Resilient(name = "some.strategy")}, pass the
 * {@code ResilienceComplianceTest} build gate, and still be completely
 * unprotected because nothing ever registered a config under that name. The
 * declaration reads as protection to the next developer; at runtime it is
 * inert. That gap is invisible today unless somebody eyeballs
 * {@code /admin/resilience} against every call site.
 *
 * <p>{@link com.naturalist.exception.UnconfiguredResilienceException} already
 * catches the same typo, but only at the moment of a facade lookup — which for
 * a rarely-exercised path means production, not integration testing. This
 * runner moves the failure to boot.
 *
 * <h2>What it does not check</h2>
 * <ul>
 *   <li>That the registered <em>primitives</em> match the call site's intent.
 *       A name registered with only a {@code TimeoutConfig} satisfies this gate
 *       even where a circuit breaker was wanted; the five
 *       {@code Resilience.*Names()} sets rendered at {@code /admin/resilience}
 *       are where that judgement is made.</li>
 *   <li>Classes that are not beans — those are the
 *       {@code ResilienceComplianceTest} build gate's territory.</li>
 *   <li>Beans not yet instantiated when the runner fires. Type resolution
 *       reports the concrete class for an eagerly-created singleton, but falls
 *       back to a {@code @Bean} method's declared return type for a
 *       {@code @Lazy} or prototype definition — so a lazily-created bean behind
 *       an interface return type would hide its declaration here. Every
 *       resilience-carrying bean in this app is an eager singleton; a future
 *       {@code @Lazy} one needs the build gate to carry it.</li>
 * </ul>
 *
 * <h2>Failure timing</h2>
 * An {@link ApplicationRunner} runs after the context refreshes and the
 * embedded server binds, so a violation kills the app a moment after the port
 * opens rather than before. Accepted deliberately: the check needs the fully
 * assembled bean set, and a boot that dies in its first second is unambiguous
 * in any deployment pipeline. Note that Spring Boot invokes runners under
 * {@code @SpringBootTest} as well, so a violation also reddens every
 * context-loading test — which is the point.
 */
final class ResilienceNameValidator implements ApplicationRunner {

    private final ApplicationContext context;
    private final Resilience resilience;

    ResilienceNameValidator(ApplicationContext context, Resilience resilience) {
        this.context = context;
        this.resilience = resilience;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> registered = registeredNames();
        Map<String, Set<String>> unresolved = new TreeMap<>();
        declarations().forEach((name, sites) -> {
            if (!registered.contains(name)) {
                unresolved.put(name, sites);
            }
        });
        if (!unresolved.isEmpty()) {
            throw new IllegalStateException(message(unresolved, registered));
        }
    }

    /**
     * Every {@code @Resilient} name declared by a registered bean, mapped to
     * the sites that declare it. Class-level and method-level declarations are
     * both collected; a bean contributes its user class, so a CGLIB-proxied
     * bean reports the annotation's real home rather than the proxy.
     */
    private Map<String, Set<String>> declarations() {
        Map<String, Set<String>> byName = new TreeMap<>();
        for (String beanName : context.getBeanDefinitionNames()) {
            Class<?> type = beanType(beanName);
            if (type == null) {
                continue;
            }
            Resilient onClass = AnnotatedElementUtils.findMergedAnnotation(type, Resilient.class);
            if (onClass != null) {
                record(byName, onClass.name(), type.getName());
            }
            ReflectionUtils.doWithMethods(type, method -> {
                Resilient onMethod = AnnotatedElementUtils.findMergedAnnotation(method, Resilient.class);
                if (onMethod != null) {
                    record(byName, onMethod.name(), type.getName() + "#" + method.getName());
                }
            }, ReflectionUtils.USER_DECLARED_METHODS);
        }
        return byName;
    }

    private Class<?> beanType(String beanName) {
        try {
            Class<?> type = context.getType(beanName, false);
            return type == null ? null : ClassUtils.getUserClass(type);
        } catch (RuntimeException unresolvable) {
            // Abstract or otherwise unresolvable definitions carry no runtime
            // call site. Skipping them cannot hide a declaration: an inert
            // definition is never invoked.
            return null;
        }
    }

    private Set<String> registeredNames() {
        Set<String> names = new TreeSet<>();
        names.addAll(resilience.retryNames());
        names.addAll(resilience.timeoutNames());
        names.addAll(resilience.circuitBreakerNames());
        names.addAll(resilience.bulkheadNames());
        names.addAll(resilience.rateLimiterNames());
        return names;
    }

    private static void record(Map<String, Set<String>> byName, String name, String site) {
        byName.computeIfAbsent(name, key -> new TreeSet<>()).add(site);
    }

    private static String message(Map<String, Set<String>> unresolved, Set<String> registered) {
        StringBuilder detail = new StringBuilder(
                "@Resilient names with no registered ResilienceConfig — these call sites "
                        + "declare protection they do not have:\n");
        unresolved.forEach((name, sites) -> detail
                .append("  \"").append(name).append("\" declared by ")
                .append(String.join(", ", sites))
                .append('\n'));
        detail.append("Registered names: ")
                .append(registered.isEmpty() ? "(none)" : String.join(", ", registered))
                .append("\nRegister a ResilienceConfig bean in ")
                .append(ResilienceConfiguration.class.getName())
                .append(" or correct the name at the call site — see docs/resilience-policy.md.");
        return detail.toString();
    }
}
