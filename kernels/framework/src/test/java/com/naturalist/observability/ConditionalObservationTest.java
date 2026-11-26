package com.naturalist.observability;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The terminal steps an endpoint branches on: {@link InvariantObservation#whenValid} answers with whichever
 * outcome applies, {@link InvariantObservation#whenNotValid} / {@code otherwise} does the same in guard order, and
 * {@link InvariantObservation#isNotValid} is the plain guard.
 */
class ConditionalObservationTest {

    private static final MethodObserver OBSERVER =
            Observer.forClass(ConditionalObservationTest.class).forMethod("observe");

    @Test
    void whenValid_noViolation_answersOnValid_only() {
        AtomicInteger notValidRan = new AtomicInteger();

        String answer = OBSERVER.state(i -> i.isTrue(true, "held"))
                .whenValid(() -> "page", () -> {
                    notValidRan.incrementAndGet();
                    return "refused";
                });

        assertThat(answer).isEqualTo("page");
        assertThat(notValidRan).hasValue(0);
    }

    @Test
    void whenValid_violation_answersOnNotValid_only() {
        AtomicInteger validRan = new AtomicInteger();

        String answer = OBSERVER.state(i -> i.isTrue(false, "violated"))
                .whenValid(() -> {
                    validRan.incrementAndGet();
                    return "page";
                }, () -> "refused");

        assertThat(answer).isEqualTo("refused");
        assertThat(validRan).hasValue(0);
    }

    @Test
    void isNotValid_answersWhetherAnyConstraintWasViolated() {
        assertThat(OBSERVER.arguments(i -> i.isTrue(true, "held")).isNotValid()).isFalse();
        assertThat(OBSERVER.arguments(i -> i.isTrue(true, "held").isTrue(false, "violated")).isNotValid()).isTrue();
    }

    @Test
    void whenNotValid_violation_answersTheRefusal_only() {
        AtomicInteger validRan = new AtomicInteger();

        String answer = OBSERVER.state(i -> i.isTrue(false, "violated"))
                .whenNotValid(() -> "refused")
                .otherwise(() -> {
                    validRan.incrementAndGet();
                    return "page";
                });

        assertThat(answer).isEqualTo("refused");
        assertThat(validRan).hasValue(0);
    }

    @Test
    void whenNotValid_noViolation_answersOtherwise_only() {
        AtomicInteger notValidRan = new AtomicInteger();

        String answer = OBSERVER.state(i -> i.isTrue(true, "held"))
                .whenNotValid(() -> {
                    notValidRan.incrementAndGet();
                    return "refused";
                })
                .otherwise(() -> "page");

        assertThat(answer).isEqualTo("page");
        assertThat(notValidRan).hasValue(0);
    }
}
