package com.naturalist.console.resilience;

import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.ResilienceConfig.TimeoutConfig;
import com.naturalist.resilience.Resilient;
import com.naturalist.resilience.resilience4j.Resilience4jResilience;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the gate detects, rather than merely staying quiet. A validator that
 * silently passes everything is indistinguishable from a working one when the
 * app happens to be correctly configured — so each case here builds a context
 * that is deliberately wrong and asserts the failure names both the strategy
 * and the site that declared it.
 *
 * <p>The real assembled context is covered separately: {@code ResilienceConfiguration}
 * registers the validator as an {@link org.springframework.boot.ApplicationRunner},
 * and Spring Boot invokes runners under {@code @SpringBootTest}, so every
 * context-loading test in this module exercises it against the production
 * bean set.
 */
class ResilienceNameValidatorTest {

    private static final Resilience RESILIENCE = new Resilience4jResilience(
            List.of(new TimeoutConfig("registered.strategy", Duration.ofMillis(500))));

    @Test
    void passesWhenEveryDeclaredNameResolves() {
        try (var context = new AnnotationConfigApplicationContext(WellConfigured.class)) {
            assertThatCode(() -> new ResilienceNameValidator(context, RESILIENCE).run(null))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void failsWhenAClassLevelDeclarationResolvesToNothing() {
        try (var context = new AnnotationConfigApplicationContext(UnregisteredOnClass.class)) {
            assertThatThrownBy(() -> new ResilienceNameValidator(context, RESILIENCE).run(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ghost.strategy")
                    .hasMessageContaining(ClassAnnotated.class.getName())
                    .hasMessageContaining("registered.strategy");
        }
    }

    @Test
    void failsWhenAMethodLevelDeclarationResolvesToNothing() {
        try (var context = new AnnotationConfigApplicationContext(UnregisteredOnMethod.class)) {
            assertThatThrownBy(() -> new ResilienceNameValidator(context, RESILIENCE).run(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ghost.strategy")
                    .hasMessageContaining(MethodAnnotated.class.getName() + "#call");
        }
    }

    @Configuration
    static class WellConfigured {
        @Bean
        ClassAnnotatedAndRegistered classAnnotatedAndRegistered() {
            return new ClassAnnotatedAndRegistered();
        }
    }

    @Configuration
    static class UnregisteredOnClass {
        @Bean
        ClassAnnotated classAnnotated() {
            return new ClassAnnotated();
        }
    }

    @Configuration
    static class UnregisteredOnMethod {
        @Bean
        MethodAnnotated methodAnnotated() {
            return new MethodAnnotated();
        }
    }

    @Resilient(name = "registered.strategy")
    static class ClassAnnotatedAndRegistered {
    }

    @Resilient(name = "ghost.strategy")
    static class ClassAnnotated {
    }

    static class MethodAnnotated {
        @Resilient(name = "ghost.strategy")
        void call() {
        }
    }
}
