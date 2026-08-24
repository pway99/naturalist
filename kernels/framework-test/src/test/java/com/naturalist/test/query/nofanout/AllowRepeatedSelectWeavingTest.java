package com.naturalist.test.query.nofanout;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;

/**
 * End-to-end proof that the gate is live via {@link NaturalistTestExtension} itself — it
 * arms the recorder in {@code beforeEach} and evaluates in {@code afterEach}, honouring
 * {@link AllowRepeatedSelect}. A looping query here would fail the test in {@code afterEach}
 * unless whitelisted; a batched query passes without a whitelist. If the wiring regressed,
 * {@code loopingQuery_isSuppressedByAnnotation} would still fail despite its annotation, and
 * removing the annotation would (correctly) turn it red — that is the invariant under test.
 */
class AllowRepeatedSelectWeavingTest {

    @RegisterExtension
    NaturalistTestExtension ext = NaturalistTestExtension.create();

    @Test
    @AllowRepeatedSelect(query = "FooQueryImpl.loadAll", select = "getByName")
    void loopingQuery_isSuppressedByAnnotation() {
        // One loadAll invocation looping getByName per element — a real N+1, whitelisted here.
        new FooQueryImpl(new FooRepositoryMock()).loadAll(List.of("a", "b", "c"));
    }

    @Test
    void batchedQuery_passesWithoutAnnotation() {
        // A single batched select — no N+1, so afterEach evaluates clean with no whitelist.
        new FooQueryImpl(new FooRepositoryMock()).loadBatched(Set.of("a", "b", "c"));
    }
}
