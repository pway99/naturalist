package com.naturalist.chemistry.compound;

import com.naturalist.data.NaturalistDatabase;

/**
 * Assembly helper for the compound sub-context. Lives in
 * {@code com.naturalist.chemistry.compound} so it can instantiate the
 * package-private {@link CompoundRepository} namespace alongside the
 * package-private mocks ({@code CompoundEntityRepositoryMock},
 * {@code DepictionEntityRepositoryMock}) and the package-private
 * {@code *QueryImpl} adapters in {@code chemistry-core}.
 */
public final class CompoundTestContext {

    private CompoundTestContext() {
    }

    public static CompoundQuery createQuery(NaturalistDatabase db) {
        CompoundRepository repository = CompoundRepository.create(
                new CompoundEntityRepositoryMock(db),
                new DepictionEntityRepositoryMock(db));
        CompoundQuery.CompoundEntityQuery compoundEntityQuery =
                new CompoundEntityQueryImpl(repository.compoundRepository());
        CompoundQuery.DepictionQuery depictionQuery =
                new DepictionQueryImpl(repository.depictionRepository());
        return new CompoundQueryImpl(compoundEntityQuery, depictionQuery);
    }
}
