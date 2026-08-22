package com.naturalist.data.count;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** End-to-end proof that the aspect weaves and feeds the recorder under load-time weaving. */
class SelectGateWeavingTest {

    @AfterEach
    void tearDown() {
        SelectCountRecorder.disarm();
    }

    @Test
    void loopingQuery_isDetectedAsAnNPlusOne() {
        SelectCountRecorder.arm();
        new FooQueryImpl(new FooRepositoryMock()).loadAll(List.of("a", "b", "c"));

        assertThatExceptionOfType(RepeatedSelectException.class)
                .isThrownBy(() -> SelectGate.evaluate(SelectCountRecorder.snapshot(), List.of()))
                .withMessageContaining("FooQueryImpl.loadAll")
                .withMessageContaining("getByName");
    }

    @Test
    void batchedQuery_passes() {
        SelectCountRecorder.arm();
        new FooQueryImpl(new FooRepositoryMock()).loadBatched(Set.of("a", "b", "c"));
        assertThatCode(() -> SelectGate.evaluate(SelectCountRecorder.snapshot(), List.of()))
                .doesNotThrowAnyException();
    }
}
