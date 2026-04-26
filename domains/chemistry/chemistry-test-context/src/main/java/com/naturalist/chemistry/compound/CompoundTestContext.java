package com.naturalist.chemistry.compound;

import com.naturalist.data.NaturalistDatabase;

/**
 * Assembly helper for the compound sub-context. Lives in
 * {@code com.naturalist.chemistry.compound} so it can instantiate the
 * package-private {@link CompoundQueryImpl} adapter alongside the
 * package-private {@link CompoundEntityRepositoryMock}.
 */
public final class CompoundTestContext {

    private CompoundTestContext() {}

    public static CompoundQuery createQuery(NaturalistDatabase db) {
        CompoundEntityRepositoryMock repository = new CompoundEntityRepositoryMock(db);
        return new CompoundQueryImpl(repository);
    }
}
