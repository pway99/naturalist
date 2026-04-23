package com.naturalist.data;

import com.naturalist.ddd.Named;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class TestEntitySourceTest<
        NAME,
        ENTITY extends Named<NAME>,
        DS extends TestEntitySource<NAME, ENTITY>> {

    @Test
    void dataLoads() throws InstantiationException, IllegalAccessException {
        TestEntitySource<NAME, ENTITY> testSource = entityClass().newInstance();
        assertThat(testSource.isEmpty()).isFalse();
    }

    @Test
    void hasAtLeastFourEntities() throws InstantiationException, IllegalAccessException {
        TestEntitySource<NAME, ENTITY> testSource = entityClass().newInstance();
        assertThat(testSource.entityStream().count())
                .as("TestEntitySource must contain at least 4 entities for meaningful repository contract coverage")
                .isGreaterThanOrEqualTo(4);
    }

    @SuppressWarnings("unchecked")
    Class<DS> entityClass() {
        return (Class<DS>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[2];
    }
}
