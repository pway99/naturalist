package com.naturalist.data;

import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.NamedEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class NamedTestEntitySourceTest<
        NAME extends EntityName<?>,
        ENTITY extends NamedEntity<NAME>,
        DS extends NamedTestEntitySource<NAME, ENTITY>> {

    @Test
    void dataLoads() throws InstantiationException, IllegalAccessException {
        NamedTestEntitySource<NAME, ENTITY> testSource = entityClass().newInstance();
        assertThat(testSource.isEmpty()).isFalse();
    }

    @Test
    void hasAtLeastFourEntities() throws InstantiationException, IllegalAccessException {
        NamedTestEntitySource<NAME, ENTITY> testSource = entityClass().newInstance();
        assertThat(testSource.entityStream().count())
                .as("NamedTestEntitySource must contain at least 4 entities for meaningful repository contract coverage")
                .isGreaterThanOrEqualTo(4);
    }

    @SuppressWarnings("unchecked")
    Class<DS> entityClass() {
        return (Class<DS>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[2];
    }
}
