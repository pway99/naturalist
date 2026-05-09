package com.naturalist.chemistry.product;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class ProductEntityRepositoryMock
        extends AbstractTestEntityRepository<ProductName, Product, ProductTestEntitySource>
        implements ProductRepository {

    protected ProductEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<Product> getByCompoundName(CompoundName compoundName) {
        observer().arguments("getByCompoundName", i -> i
                        .identifier(compoundName, "compoundName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(product -> product.compounds().contains(compoundName))
                .toList();
    }
}
