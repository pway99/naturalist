package com.naturalist.chemistry.compound;

import com.naturalist.data.NaturalistDatabase;

/**
 * Test-scope wiring for {@link CompoundQuery.CompoundEntityQuery}. Lives in
 * {@code com.naturalist.chemistry.compound} so it can instantiate the
 * package-private {@link CompoundEntityRepositoryMock} and
 * {@link CompoundEntityQueryImpl} without exposing either to the wider
 * test classpath.
 * <p>
 * Mirrors the production {@code CompoundTestContext} (in
 * {@code chemistry-test-context}); duplicated here because {@code chemistry-core}
 * cannot depend on {@code chemistry-test-context} without forming a reactor cycle.
 */
public final class CompoundQueryTestContextInternal {

    private CompoundQueryTestContextInternal() {
    }

    public static CompoundQuery.CompoundEntityQuery createEntityQuery(NaturalistDatabase db) {
        return new CompoundEntityQueryImpl(new CompoundEntityRepositoryMock(db));
    }
}
