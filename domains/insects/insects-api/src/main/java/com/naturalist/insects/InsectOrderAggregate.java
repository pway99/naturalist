package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Order-rank {@link InsectAggregate} — used when only order is resolved
 * (e.g. <i>Diptera</i> sp.). Composes the order record with the photographic
 * field record. Root identity is the order's {@link InsectOrderName}.
 */
public record InsectOrderAggregate(
        InsectOrder order,
        ImageCollection images
) implements InsectAggregate {

    public static InsectOrderAggregate of(InsectOrder order, ImageCollection images) {
        return new InsectOrderAggregate(order, images);
    }

    public static InsectOrderAggregate of(InsectOrder order) {
        return new InsectOrderAggregate(order, ImageCollection.empty());
    }

    @Override
    public InsectOrderName name() {
        return order.name();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(order, "order")
                .behavioralCollection(images, "images");
    }
}
