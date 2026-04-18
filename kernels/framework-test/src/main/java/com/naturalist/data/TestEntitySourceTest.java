package com.naturalist.data;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.PersistenceId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class TestEntitySourceTest<ID extends PersistenceId<?>, NAME extends EntityName<?>, ENTITY extends Entity<ID, NAME>, DS extends TestEntitySource<ID, NAME, ENTITY>> {
    @Test
    void dataLoads() throws InstantiationException, IllegalAccessException {
        TestEntitySource<ID, NAME, ENTITY> testSource = entityClass().newInstance();
        assertThat(testSource.isEmpty()).isFalse();
    }

    @Test
    void hasAtLeastFourEntities() throws InstantiationException, IllegalAccessException {
        TestEntitySource<ID, NAME, ENTITY> testSource = entityClass().newInstance();
        assertThat(testSource.entityStream().count())
                .as("TestEntitySource must contain at least 4 entities for meaningful repository contract coverage")
                .isGreaterThanOrEqualTo(4);
    }

    Class<DS> entityClass() {
        return (Class<DS>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[3];
    }

}
