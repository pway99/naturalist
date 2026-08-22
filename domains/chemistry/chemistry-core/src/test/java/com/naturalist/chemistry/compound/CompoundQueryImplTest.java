package com.naturalist.chemistry.compound;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompoundQueryImplTest {

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    CompoundEntityRepositoryMock compoundRepository = new CompoundEntityRepositoryMock(db);
    DepictionEntityRepositoryMock depictionRepository = new DepictionEntityRepositoryMock(db);
    CompoundQuery.CompoundEntityQuery compoundEntityQuery = new CompoundEntityQueryImpl(compoundRepository);
    CompoundQuery.DepictionQuery depictionQuery = new DepictionQueryImpl(depictionRepository);
    CompoundQuery compoundQuery = new CompoundQueryImpl(compoundEntityQuery, depictionQuery);

    @Test
    void accessors_returnNonNullDelegates() {
        assertThat(compoundQuery.compounds()).isSameAs(compoundEntityQuery);
        assertThat(compoundQuery.depictions()).isSameAs(depictionQuery);
    }

    @Test
    void accessors_idempotent() {
        assertThat(compoundQuery.compounds()).isSameAs(compoundQuery.compounds());
        assertThat(compoundQuery.depictions()).isSameAs(compoundQuery.depictions());
    }

    @Test
    void constructor_rejectsNullCompoundEntityQuery() {
        assertThatThrownBy(() -> new CompoundQueryImpl(null, depictionQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("compoundEntityQuery");
    }

    @Test
    void constructor_rejectsNullDepictionQuery() {
        assertThatThrownBy(() -> new CompoundQueryImpl(compoundEntityQuery, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("depictionQuery");
    }

    @Test
    void constructor_collectsAllViolationsInSinglePass() {
        assertThatThrownBy(() -> new CompoundQueryImpl(null, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("compoundEntityQuery", "depictionQuery");
    }
}
