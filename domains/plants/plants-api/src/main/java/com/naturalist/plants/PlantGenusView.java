package com.naturalist.plants;

import com.naturalist.plants.PlantEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;

/** Genus-rank {@link PlantTaxonView}. Root identity is the genus's {@link PlantGenusName}. */
public record PlantGenusView(PlantGenus genus, ImageCollection images) implements PlantTaxonView {

    public static PlantGenusView of(PlantGenus genus, ImageCollection images) {
        return new PlantGenusView(genus, images);
    }

    public static PlantGenusView of(PlantGenus genus) {
        return new PlantGenusView(genus, ImageCollection.empty());
    }

    @Override public PlantGenusName name() { return genus.name(); }

    /** The family this genus belongs to, exposed as a typed FK delegate. */
    public PlantFamilyName familyName() { return genus.familyName(); }

    /** True iff this genus's family FK equals the given family's name, OR the family is null. */
    public boolean belongsToFamily(@Nullable PlantFamilyView family) {
        return family == null || genus.familyName().equals(family.name());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.namedEntity(genus, "genus").behavioralCollection(images, "images");
    }
}
