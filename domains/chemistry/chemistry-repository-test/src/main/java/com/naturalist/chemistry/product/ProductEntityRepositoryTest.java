package com.naturalist.chemistry.product;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        // thymol is a real catalogued compound — satisfies the RDBMS adapter's
        // product_compound FK (the mock ignores it); the mock is unaffected.
        return new Product(
                ProductName.of(RandomValue.string()),
                RandomValue.string(),
                Set.of(TestChemistryIdentifiers.Compounds.Thymol.name),
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
                Set.of(TestChemistryIdentifiers.Compounds.Thymol.name),
                Map.of(RandomValue.string(), RandomValue.string()));
    }

    // =========================================================================
    // getByCompoundName — reverse lookup from compound to its products
    // =========================================================================

    @Test
    default void getByCompoundName_nullArgument() {
        assertThatThrownBy(() -> repository().getByCompoundName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("compoundName");
    }

    @Test
    default void getByCompoundName_unknownCompound_returnsEmpty() {
        assertThat(repository().getByCompoundName(TestChemistryIdentifiers.Compounds.NotFound.name))
                .isEmpty();
    }

    @Test
    default void getByCompoundName_knownCompound_returnsContainingProducts() {
        CompoundName thymol = TestChemistryIdentifiers.Compounds.Thymol.name;

        List<Product> result = repository().getByCompoundName(thymol);

        assertThat(result)
                .extracting(Product::name)
                .contains(TestChemistryIdentifiers.Products.Apiguard.name);
        assertThat(result).allSatisfy(product ->
                assertThat(product.compounds()).contains(thymol));
    }
}
