package com.naturalist.chemistry.product;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public final class ProductCollection extends BehavioralCollection<Product> {

    ProductCollection(Collection<Product> products) {
        super(products);
    }

    public static ProductCollection of(Collection<Product> products) {
        return new ProductCollection(products);
    }

    public static ProductCollection empty() {
        return new ProductCollection(List.of());
    }
}
