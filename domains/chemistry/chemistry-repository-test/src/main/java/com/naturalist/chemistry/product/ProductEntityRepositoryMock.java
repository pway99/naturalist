package com.naturalist.chemistry.product;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;

@MockDomainService
class ProductEntityRepositoryMock
        extends AbstractTestEntityRepository<ProductName, Product, ProductTestEntitySource>
        implements ProductRepository {

    protected ProductEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<Product> getByCompoundName(CompoundName compoundName) {
        MethodObserver getByCompoundName = observer().forMethod("getByCompoundName");
        getByCompoundName.arguments(i -> i
                        .identifier(compoundName, "compoundName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(product -> product.compounds().contains(compoundName))
                .toList();
        getByCompoundName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
