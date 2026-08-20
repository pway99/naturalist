package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Order-rank {@link InsectTaxonView} — used when only order is resolved
 * (e.g. <i>Diptera</i> sp.). Composes the order record with the photographic
 * field record. Root identity is the order's {@link InsectOrderName}.
 */
public record InsectOrderView(
        InsectOrder order,
        ImageCollection images
) implements InsectTaxonView {

    public static InsectOrderView of(InsectOrder order, ImageCollection images) {
        return new InsectOrderView(order, images);
    }

    public static InsectOrderView of(InsectOrder order) {
        return new InsectOrderView(order, ImageCollection.empty());
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
