package com.naturalist.chemistry.product;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Behavioral contract for {@link ProductRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies Product-specific identity constants and entity construction.
 */
interface ProductEntityRepositoryTest
        extends EntityRepositoryTest<ProductName, Product> {

    @Override
    ProductRepository repository();

    @Override
    default TestEntitySource<ProductName, Product> source() {
        return db.getNamed(ProductTestEntitySource.class);
    }

    @Override
    default ProductName notFoundName() {
        return TestChemistryIdentifiers.Products.NotFound.name;
    }

    @Override
    default List<ProductName> knownEntityNames() {
        return List.of(
                TestChemistryIdentifiers.Products.Apiguard.name,
                TestChemistryIdentifiers.Products.TpsCalmagOac.name);
    }

    @Override
    default Product newEntity() {
        return new Product(
                ProductName.of(RandomValue.string()),
                RandomValue.string(),
                Set.of(CompoundName.of(RandomValue.string())),
                Map.of(RandomValue.string(), RandomValue.string()));
    }

    @Override
    default Product ghostEntity() {
        return new Product(
                ProductName.of(RandomValue.string()),
                RandomValue.string(),
                Set.of(CompoundName.of(RandomValue.string())),
                Map.of(RandomValue.string(), RandomValue.string()));
    }

    @Override
    default Product modifiedEntity(Product original) {
        return new Product(
                original.name(),
                RandomValue.string(),
                Set.of(CompoundName.of(RandomValue.string())),
                Map.of(RandomValue.string(), RandomValue.string()));
    }
}
