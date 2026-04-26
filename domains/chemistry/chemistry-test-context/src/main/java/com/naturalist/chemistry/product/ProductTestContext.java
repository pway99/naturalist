package com.naturalist.chemistry.product;

import com.naturalist.data.NaturalistDatabase;

/**
 * Assembly helper for the product sub-context. Lives in
 * {@code com.naturalist.chemistry.product} so it can instantiate the
 * package-private {@link ProductRepository} alongside the package-private
 * {@code ProductEntityRepositoryMock} and the package-private
 * {@code ProductQueryImpl} adapter in {@code chemistry-core}.
 */
public final class ProductTestContext {

    private ProductTestContext() {}

    public static ProductQuery createQuery(NaturalistDatabase db) {
        ProductRepository repository = new ProductEntityRepositoryMock(db);
        return new ProductQueryImpl(repository);
    }
}
