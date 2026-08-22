package com.naturalist.data.count;

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
