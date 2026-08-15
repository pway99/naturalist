package com.naturalist.chemistry.element;

import com.naturalist.data.NaturalistDatabase;

/**
 * Assembly helper for the element sub-context. Lives in
 * {@code com.naturalist.chemistry.element} so it can instantiate the
 * package-private {@link ElementRepository} implementation
 * ({@code ElementEntityRepositoryMock}) and the package-private
 * {@code ElementQueryImpl} adapter in {@code chemistry-core}.
 * <p>
 * Mirrors {@code ProductTestContext} — one entity in the package, so the
 * N=1 collapse applies and there is no namespace to assemble.
 */
public final class ElementTestContext {

    private ElementTestContext() {
    }

    public static ElementQuery createQuery(NaturalistDatabase db) {
        return new ElementQueryImpl(new ElementEntityRepositoryMock(db));
    }
}
