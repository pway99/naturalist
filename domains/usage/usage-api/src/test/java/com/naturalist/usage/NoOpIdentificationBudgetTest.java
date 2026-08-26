package com.naturalist.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.naturalist.naturalist.NaturalistName;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NoOpIdentificationBudgetTest {

    @Test
    void reserve_does_not_throw() {
        assertThatCode(() -> IdentificationBudget.noOp().reserve(NaturalistName.of("pat")))
                .doesNotThrowAnyException();
    }

    @Test
    void budgetExceededException_accessors_return_what_was_passed() {
        var resetAt = Instant.parse("2026-08-25T00:00:00Z");
        var exception = new BudgetExceededException(LimitKind.MONTHLY, resetAt);

        assertThat(exception.limitKind()).isEqualTo(LimitKind.MONTHLY);
        assertThat(exception.resetAt()).isEqualTo(resetAt);
    }
}
