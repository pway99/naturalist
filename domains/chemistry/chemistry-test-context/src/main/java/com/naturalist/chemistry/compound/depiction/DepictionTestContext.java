package com.naturalist.chemistry.compound.depiction;

import com.naturalist.data.NaturalistDatabase;

/**
 * Assembly helper for the depiction sub-context. Lives in
 * {@code com.naturalist.chemistry.compound.depiction} so it can instantiate the
 * package-private {@link DepictionQueryImpl} adapter alongside the
 * package-private {@link DepictionEntityRepositoryMock}.
 */
public final class DepictionTestContext {

    private DepictionTestContext() {}

    public static DepictionQuery createQuery(NaturalistDatabase db) {
        DepictionEntityRepositoryMock repository = new DepictionEntityRepositoryMock(db);
        return new DepictionQueryImpl(repository);
    }
}
