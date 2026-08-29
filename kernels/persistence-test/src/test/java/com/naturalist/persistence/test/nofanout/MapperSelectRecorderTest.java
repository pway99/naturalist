package com.naturalist.persistence.test.nofanout;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MapperSelectRecorderTest {

    @BeforeEach
    void arm() {
        MapperSelectRecorder.arm();
    }

    @AfterEach
    void disarm() {
        MapperSelectRecorder.disarm();
    }

    @Test
    void repeatedSelectInOneHeadTalliesTwo() {
        MapperSelectRecorder.enterRepository("Repo.getByNameSet");
        MapperSelectRecorder.recordSelect("Mapper.selectById");
        MapperSelectRecorder.recordSelect("Mapper.selectById");
        MapperSelectRecorder.exitRepository();

        assertThat(MapperSelectRecorder.snapshot())
                .containsEntry("Repo.getByNameSet", Map.of("Mapper.selectById", 2));
    }

    @Test
    void repeatedInvocationsEachOneSelectFoldToOne() {
        for (int i = 0; i < 3; i++) {
            MapperSelectRecorder.enterRepository("Repo.getByName");
            MapperSelectRecorder.recordSelect("Mapper.selectById");
            MapperSelectRecorder.exitRepository();
        }

        assertThat(MapperSelectRecorder.snapshot())
                .containsEntry("Repo.getByName", Map.of("Mapper.selectById", 1));
    }

    @Test
    void twoDistinctSelectsInOneHeadAreEachCountOne() {
        MapperSelectRecorder.enterRepository("Repo.getPage");
        MapperSelectRecorder.recordSelect("Mapper.selectPage");
        MapperSelectRecorder.recordSelect("Mapper.countInWindow");
        MapperSelectRecorder.exitRepository();

        assertThat(MapperSelectRecorder.snapshot())
                .containsEntry("Repo.getPage", Map.of("Mapper.selectPage", 1, "Mapper.countInWindow", 1));
    }

    @Test
    void recordWhenDisarmedIsIgnored() {
        MapperSelectRecorder.disarm(); // fresh scope, armed == false
        MapperSelectRecorder.enterRepository("Repo.x");
        MapperSelectRecorder.recordSelect("Mapper.y");
        MapperSelectRecorder.exitRepository();

        assertThat(MapperSelectRecorder.snapshot()).isEmpty();
    }
}
