package com.naturalist.chemistry.compound;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

@DomainService
class CompoundQueryImpl implements CompoundQuery {

    private final CompoundEntityQuery compoundEntityQuery;
    private final DepictionQuery depictionQuery;

    CompoundQueryImpl(CompoundEntityQuery compoundEntityQuery, DepictionQuery depictionQuery) {
        Observer.forClass(CompoundQueryImpl.class).arguments("constructor", i -> i
                        .notNull(compoundEntityQuery, "compoundEntityQuery")
                        .notNull(depictionQuery, "depictionQuery"))
                .throwWhenInvalid();
        this.compoundEntityQuery = compoundEntityQuery;
        this.depictionQuery = depictionQuery;
    }

    @Override
    public CompoundEntityQuery compounds() {
        return compoundEntityQuery;
    }

    @Override
    public DepictionQuery depictions() {
        return depictionQuery;
    }
}
