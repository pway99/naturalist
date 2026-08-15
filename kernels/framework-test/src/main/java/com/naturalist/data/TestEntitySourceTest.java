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
    void dataLoads() {
        TestEntitySource<NAME, ENTITY> testSource = NaturalistDatabase.create().getNamed(entityClass());
        assertThat(testSource.isEmpty()).isFalse();
    }

    @Test
    void hasAtLeastMinimumEntities() {
        TestEntitySource<NAME, ENTITY> testSource = NaturalistDatabase.create().getNamed(entityClass());
        assertThat(testSource.entityStream().count())
                .as("TestEntitySource must contain at least %d entities for meaningful repository "
                        + "contract coverage", minimumEntities())
                .isGreaterThanOrEqualTo(minimumEntities());
    }

    /**
     * The fixture floor. Four by default: the repository contract tests page at size 2, so four
     * entities exercise multi-page boundaries and set lookups meaningfully.
     * <p>
     * Override <b>only</b> when the domain genuinely holds fewer than four real instances — Oak
     * Vista has two sampled soil units, so its profile, analysis, and physical-characteristics
     * catalogs each hold two rows. Fixtures carry real measurements
     * (see {@code domains/CLAUDE.md}); padding a catalog with invented entities to clear this bar
     * defeats the rule rather than satisfying it, and an invented lab analysis is a worse fixture
     * than a small one. A source that lowers this floor should also lower
     * {@code EntityRepositoryTest.pageSize()} so multi-page behaviour is still covered.
     */
    protected int minimumEntities() {
        return 4;
    }

    @SuppressWarnings("unchecked")
    Class<DS> entityClass() {
        return (Class<DS>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[2];
    }
}
