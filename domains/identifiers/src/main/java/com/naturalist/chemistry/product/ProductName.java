package com.naturalist.chemistry.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Natural key for {@code Product} — a human-readable slug stable across deployments.
 * <p>
 * A product is a commercial formulation that contains one or more compounds
 * (e.g. {@code "tps-calmag-oac"}, {@code "apiguard"}). Compounds reference products
 * indirectly: lookups go through the product query rather than embedded references.
 * <p>
 * Example: {@code ProductName.of("apiguard")}
 */
public final class ProductName extends EntityName {

    private ProductName(String value) {
        super(value);
    }

    @JsonCreator
    public static ProductName of(String value) {
        return new ProductName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
