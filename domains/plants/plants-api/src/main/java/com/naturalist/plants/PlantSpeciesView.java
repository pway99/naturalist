package com.naturalist.plants;

import com.naturalist.plants.PlantEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;

/** Species-rank {@link PlantTaxonView}. Root identity is the species's {@link PlantSpeciesName}. */
public record PlantSpeciesView(PlantSpecies species, ImageCollection images) implements PlantTaxonView {

    public static PlantSpeciesView of(PlantSpecies species, ImageCollection images) {
        return new PlantSpeciesView(species, images);
    }

    public static PlantSpeciesView of(PlantSpecies species) {
        return new PlantSpeciesView(species, ImageCollection.empty());
    }

    @Override public PlantSpeciesName name() { return species.name(); }

    /** The genus this species belongs to, exposed as a typed FK delegate. */
    public PlantGenusName genusName() { return species.genusName(); }

    /** True iff this species's genus FK equals the given genus's name, OR the genus is null. */
    public boolean belongsToGenus(@Nullable PlantGenusView genus) {
        return genus == null || species.genusName().equals(genus.name());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.namedEntity(species, "species").behavioralCollection(images, "images");
    }
}
