package com.naturalist.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestEntitySourceMapperTest {
    @Test
    void newBaseMapperIsAFreshInstanceEachCall() {
        ObjectMapper a = TestDataHelper.newBaseMapper();
        ObjectMapper b = TestDataHelper.newBaseMapper();
        assertThat(a).isNotSameAs(b);
    }
}
