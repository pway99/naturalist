package com.naturalist.chemistry.compound;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompoundQueryImplTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Wired CompoundQuery.CompoundEntityQuery compoundEntityQuery;
    @Wired CompoundQuery.DepictionQuery depictionQuery;
    @Wired CompoundQuery compoundQuery;

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
