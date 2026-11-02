package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Order-rank permit of {@link InsectTaxon} — used when only order is resolved (e.g. <i>Diptera</i> sp.).
 * Composes the order record with the photographic field record. Root identity is the
 * order's {@link InsectOrderName}.
 */
public record InsectOrder(
        InsectOrderInfo info,
        ImageCollection images
) implements InsectTaxon {

    public static InsectOrder of(InsectOrderInfo order, ImageCollection images) {
        return new InsectOrder(order, images);
    }

    public static InsectOrder of(InsectOrderInfo order) {
        return new InsectOrder(order, ImageCollection.empty());
    }

    @Override
    public InsectOrderName name() {
        return info.name();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(info, "info")
                .behavioralCollection(images, "images");
    }
}
