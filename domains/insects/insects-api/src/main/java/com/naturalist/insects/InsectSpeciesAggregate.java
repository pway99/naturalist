package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Species-rank {@link InsectAggregate} — the canonical "we know exactly what species
 * this is" view. Composes the species record with the photographic field record. Root
 * identity is the species's {@link InsectSpeciesName}.
 */
public record InsectSpeciesAggregate(
        InsectSpecies species,
        ImageCollection images
) implements InsectAggregate {

    public static InsectSpeciesAggregate of(InsectSpecies species, ImageCollection images) {
        return new InsectSpeciesAggregate(species, images);
    }

    public static InsectSpeciesAggregate of(InsectSpecies species) {
        return new InsectSpeciesAggregate(species, ImageCollection.empty());
    }

    @Override
    public InsectRankName name() {
        return species.name();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(species, "species")
                .observable(images, "images");
    }
}
