package com.naturalist.chemistry.product;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.EntityRepository;

import java.util.List;

interface ProductRepository extends EntityRepository<ProductName, Product> {

    List<Product> getByCompoundName(CompoundName compoundName);
}
