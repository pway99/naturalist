package com.naturalist.chemistry.product;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.EntityQuery;
import com.naturalist.ddd.EntityNameSet;

public interface ProductQuery extends EntityQuery<ProductName, Product, ProductCollection> {

    /**
     * Find all products that contain the given compound in their formulation.
     * The relationship is stored on {@link Product#compounds()} — products are the
     * authoritative side of compound↔product. Returns an empty collection when no
     * product references the compound.
     */
    ProductCollection findByCompoundName(CompoundName compoundName);

    EntityNameSet<ProductName> allProductNames();
}
