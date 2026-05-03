package com.naturalist.chemistry.product;

import com.naturalist.data.NaturalistDatabase;

/**
 * Test-scope wiring for {@link ProductQuery}. Lives in
 * {@code com.naturalist.chemistry.product} so it can instantiate the
 * package-private {@link ProductEntityRepositoryMock} and
 * {@link ProductQueryImpl} without exposing either to the wider test classpath.
 * <p>
 * Mirrors the production {@code ProductTestContext} (in
 * {@code chemistry-test-context}); duplicated here because {@code chemistry-core}
 * cannot depend on {@code chemistry-test-context} without forming a reactor cycle.
 */
public final class ProductQueryTestSupport {

    private ProductQueryTestSupport() {
    }

    public static ProductQuery createQuery(NaturalistDatabase db) {
        return new ProductQueryImpl(new ProductEntityRepositoryMock(db));
    }
}
