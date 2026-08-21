package com.naturalist.plants;

import com.naturalist.plants.PlantEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import java.util.function.Consumer;

/** Order-rank {@link PlantTaxonView}. Root identity is the order's {@link PlantOrderName}. */
public record PlantOrderView(PlantOrder order, ImageCollection images) implements PlantTaxonView {

    public static PlantOrderView of(PlantOrder order, ImageCollection images) {
        return new PlantOrderView(order, images);
    }

    public static PlantOrderView of(PlantOrder order) {
        return new PlantOrderView(order, ImageCollection.empty());
    }

    @Override public PlantOrderName name() { return order.name(); }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.namedEntity(order, "order").behavioralCollection(images, "images");
    }
}
