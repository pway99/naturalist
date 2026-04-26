package com.naturalist.chemistry.product;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

class ProductEntityRepositoryMock
        extends AbstractTestEntityRepository<ProductName, Product, ProductTestEntitySource>
        implements ProductRepository {

    protected ProductEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
