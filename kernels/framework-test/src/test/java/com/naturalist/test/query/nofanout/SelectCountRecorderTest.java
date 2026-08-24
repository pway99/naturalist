package com.naturalist.test.query.nofanout;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SelectCountRecorderTest {

    @AfterEach
    void tearDown() {
        SelectCountRecorder.disarm();
    }

    @Test
    void repeatedSelect_withinAHead_isTallied() {
        SelectCountRecorder.arm();
        SelectCountRecorder.enterQuery("FooQueryImpl.loadAll");
        SelectCountRecorder.recordSelect("FooRepositoryMock.getByName");
        SelectCountRecorder.recordSelect("FooRepositoryMock.getByName");
        SelectCountRecorder.exitQuery();

        assertThat(SelectCountRecorder.snapshot())
                .containsEntry("FooQueryImpl.loadAll", Map.of("FooRepositoryMock.getByName", 2));
    }

    @Test
    void separateInvocationsOfTheSameHead_areNotMergedIntoAViolation() {
        // A test (or caller) that invokes the SAME query method twice, each doing ONE select,
        // is NOT an N+1: the fan-out rule is per head-of-DAG INVOCATION, not per-FQN-across-the-test.
        // The two invocations must not sum to a phantom "getByParentName x2".
        SelectCountRecorder.arm();
        SelectCountRecorder.enterQuery("FooQueryImpl.forParentName");
        SelectCountRecorder.recordSelect("FooRepositoryMock.getByParentName");
        SelectCountRecorder.exitQuery();
        SelectCountRecorder.enterQuery("FooQueryImpl.forParentName");
        SelectCountRecorder.recordSelect("FooRepositoryMock.getByParentName");
        SelectCountRecorder.exitQuery();

        assertThat(SelectCountRecorder.snapshot())
                .containsEntry("FooQueryImpl.forParentName", Map.of("FooRepositoryMock.getByParentName", 1));
    }

    @Test
    void repeatedSelectWithinOneInvocation_isStillTallied() {
        // Contrast with the above: a single invocation looping the select IS an N+1.
        SelectCountRecorder.arm();
        SelectCountRecorder.enterQuery("FooQueryImpl.findByRankName");
        SelectCountRecorder.recordSelect("FooRepositoryMock.getBySubject");
        SelectCountRecorder.recordSelect("FooRepositoryMock.getBySubject");
        SelectCountRecorder.recordSelect("FooRepositoryMock.getBySubject");
        SelectCountRecorder.exitQuery();

        assertThat(SelectCountRecorder.snapshot())
                .containsEntry("FooQueryImpl.findByRankName", Map.of("FooRepositoryMock.getBySubject", 3));
    }

    @Test
    void selectWithNoEnclosingQuery_isNotTallied() {
        SelectCountRecorder.arm();
        SelectCountRecorder.recordSelect("FooRepositoryMock.getByName"); // arrange/assert call — depth 0
        assertThat(SelectCountRecorder.snapshot()).isEmpty();
    }

    @Test
    void selectWhenNotArmed_isNotTallied() {
        SelectCountRecorder.enterQuery("FooQueryImpl.loadAll");
        SelectCountRecorder.recordSelect("FooRepositoryMock.getByName");
        SelectCountRecorder.exitQuery();
        assertThat(SelectCountRecorder.snapshot()).isEmpty();
    }

    @Test
    void nestedQueries_attributeSelectsToTheOutermostHead() {
        SelectCountRecorder.arm();
        SelectCountRecorder.enterQuery("OuterQueryImpl.forOrder");
        SelectCountRecorder.enterQuery("InnerQueryImpl.forFamily");
        SelectCountRecorder.recordSelect("InnerRepositoryMock.getByParentName");
        SelectCountRecorder.exitQuery();
        SelectCountRecorder.exitQuery();

        assertThat(SelectCountRecorder.snapshot())
                .containsOnlyKeys("OuterQueryImpl.forOrder");
    }

    @Test
    void disarm_clearsAllState() {
        SelectCountRecorder.arm();
        SelectCountRecorder.enterQuery("FooQueryImpl.loadAll");
        SelectCountRecorder.recordSelect("FooRepositoryMock.getByName");
        SelectCountRecorder.disarm();
        assertThat(SelectCountRecorder.snapshot()).isEmpty();
    }
}
