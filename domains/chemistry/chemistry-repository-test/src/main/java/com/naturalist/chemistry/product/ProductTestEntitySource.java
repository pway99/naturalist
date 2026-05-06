package com.naturalist.chemistry.product;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class ProductTestEntitySource extends TestEntitySource<ProductName, Product> {
    public ProductTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFiles("chemistry/product/products-%s.json", "base");
    }

    @Override
    protected List<UniqueConstraint<Product>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "displayName";
                    }

                    @Override
                    public Function<Product, ?> valueFunction() {
                        return Product::displayName;
                    }
                }
        );
    }
}
