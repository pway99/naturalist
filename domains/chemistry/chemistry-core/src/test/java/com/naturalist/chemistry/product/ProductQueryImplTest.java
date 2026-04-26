package com.naturalist.chemistry.product;

import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.data.NaturalistDatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductQueryImplTest {
    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    ProductRepository repository = new ProductEntityRepositoryMock(db);
    ProductQuery productQuery = new ProductQueryImpl(repository);

    @Test
    void findByNameSet_happyPath() {
        Set<ProductName> productNames = Set.of(
                TestChemistryIdentifiers.Products.Apiguard.name,
                TestChemistryIdentifiers.Products.TpsCalmagOac.name
        );
        List<Product> expected = repository.getByEntityNameSet(productNames);
        assertThat(expected)
                .hasSize(2)
                .extracting(Product::name)
                .containsAnyElementsOf(productNames);

        ProductCollection pc = productQuery.findByNameSet(productNames);

        assertThat(pc).isNotNull();
        assertThat(pc.size()).isEqualTo(2);
        assertThat(pc.stream().toList())
                .containsExactlyElementsOf(expected);
    }
}
