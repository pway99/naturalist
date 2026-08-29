package com.naturalist.persistence.test.nofanout;

import com.naturalist.test.query.nofanout.AllowRepeatedSelect;
import com.naturalist.test.query.nofanout.RepeatedSelectException;
import com.naturalist.test.query.nofanout.SelectGate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end proof that {@link RepositoryHeadAspect} is load-time woven around repository methods
 * and scopes mapper selects per head. If weaving does not happen, no head is pushed,
 * {@code recordSelect} early-returns, and {@code loopedSelectIsFlagged} fails to throw — so this
 * test also guards the aspect wiring itself.
 */
class RepositoryHeadWeavingTest {

    private final GadgetRepositoryRdbms repo = new GadgetRepositoryRdbms();

    @BeforeEach
    void arm() {
        MapperSelectRecorder.arm();
    }

    @AfterEach
    void disarm() {
        MapperSelectRecorder.disarm();
    }

    @Test
    void loopedSelectIsFlagged() {
        repo.fannedOut(Set.of("a", "b", "c"));

        assertThatThrownBy(() -> SelectGate.evaluate(MapperSelectRecorder.snapshot(), List.of()))
                .isInstanceOf(RepeatedSelectException.class)
                .hasMessageContaining(GadgetRepositoryRdbms.SELECT);
    }

    @Test
    void batchedSelectPasses() {
        repo.batched(Set.of("a", "b", "c"));

        assertThatCode(() -> SelectGate.evaluate(MapperSelectRecorder.snapshot(), List.of()))
                .doesNotThrowAnyException();
    }

    @Test
    @AllowRepeatedSelect(query = "fannedOut", select = "selectById")
    void allowlistedRepeatIsSuppressed(TestInfo info) {
        repo.fannedOut(Set.of("a", "b"));

        List<AllowRepeatedSelect> allowlist = Arrays.asList(
                info.getTestMethod().orElseThrow().getAnnotationsByType(AllowRepeatedSelect.class));

        assertThatCode(() -> SelectGate.evaluate(MapperSelectRecorder.snapshot(), allowlist))
                .doesNotThrowAnyException();
    }
}
