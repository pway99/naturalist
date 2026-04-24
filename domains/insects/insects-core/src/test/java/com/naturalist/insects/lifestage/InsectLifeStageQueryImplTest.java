package com.naturalist.insects.lifestage;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectLifeStageQueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    LifeStageEntityRepositoryMock repository = new LifeStageEntityRepositoryMock(db);
    InsectLifeStageQuery.LifeStageEntityQuery lifeStageEntityQuery = new LifeStageEntityQueryImpl(repository);
    InsectLifeStageQuery query = new InsectLifeStageQueryImpl(lifeStageEntityQuery);

    @Test
    void accessors_returnNonNullDelegates() {
        assertThat(query.lifeStages()).isSameAs(lifeStageEntityQuery);
    }

    @Test
    void accessors_idempotent() {
        assertThat(query.lifeStages()).isSameAs(query.lifeStages());
    }

    @Test
    void constructor_rejectsNullLifeStageEntityQuery() {
        assertThatThrownBy(() -> new InsectLifeStageQueryImpl(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("lifeStageEntityQuery");
    }
}