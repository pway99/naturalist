package com.naturalist.plants;

import com.naturalist.plants.PlantEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;

/** Family-rank {@link PlantTaxonView}. Root identity is the family's {@link PlantFamilyName}. */
public record PlantFamilyView(PlantFamily family, ImageCollection images) implements PlantTaxonView {

    public static PlantFamilyView of(PlantFamily family, ImageCollection images) {
        return new PlantFamilyView(family, images);
    }

    public static PlantFamilyView of(PlantFamily family) {
        return new PlantFamilyView(family, ImageCollection.empty());
    }

    @Override public PlantFamilyName name() { return family.name(); }

    /** The order this family belongs to, exposed as a typed FK delegate. */
    public PlantOrderName orderName() { return family.orderName(); }

    /** True iff this family's order FK equals the given order's name, OR the order is null. */
    public boolean belongsToOrder(@Nullable PlantOrderView order) {
        return order == null || family.orderName().equals(order.name());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.namedEntity(family, "family").behavioralCollection(images, "images");
    }
}
