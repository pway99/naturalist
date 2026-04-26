package com.naturalist.chemistry.product;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.ddd.EntityNameSet;
import com.naturalist.infrastructure.DomainService;

import java.util.Set;

@DomainService
class ProductQueryImpl
        extends AbstractEntityQuery<ProductName, Product, ProductCollection, ProductRepository>
        implements ProductQuery {

    ProductQueryImpl(ProductRepository repository) {
        super(repository);
    }

    @Override
    public ProductCollection findByNameSet(Set<ProductName> productNames) {
        observer().arguments("findByNameSet", i -> i
                .identifierSet(productNames, "productNames"))
                .throwWhenInvalid();

        return new ProductCollection(repository().getByEntityNameSet(productNames));
    }

    @Override
    public ProductCollection findByCompoundName(CompoundName compoundName) {
        observer().arguments("findByCompoundName", i -> i
                .identifier(compoundName, "compoundName"))
                .throwWhenInvalid();

        return new ProductCollection(repository().getByCompoundName(compoundName));
    }

    @Override
    public EntityNameSet<ProductName> allProductNames() {
        return EntityNameSet.of(repository().getAllProductNames());
    }
}
