package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Species-rank {@link InsectTaxonView} — the canonical "we know exactly what species
 * this is" view. Composes the species record with the photographic field record. Root
 * identity is the species's {@link InsectSpeciesName}.
 */
public record InsectSpeciesView(
        InsectSpecies species,
        ImageCollection images
) implements InsectTaxonView {

    public static InsectSpeciesView of(InsectSpecies species, ImageCollection images) {
        return new InsectSpeciesView(species, images);
    }

    public static InsectSpeciesView of(InsectSpecies species) {
        return new InsectSpeciesView(species, ImageCollection.empty());
    }

    @Override
    public InsectSpeciesName name() {
        return species.name();
    }

    /** The genus this species belongs to, exposed as a typed FK delegate. */
    public InsectGenusName genusName() {
        return species.genusName();
    }

    /**
     * True iff this species's genus FK equals the given genus's name, OR the
     * given genus is null. Null tolerance lets a caller compose this check
     * inside a {@code whenNotNull(species, ...)} block without firing a
     * redundant {@code isTrue} violation when the genus is missing.
     */
    public boolean belongsToGenus(@Nullable InsectGenusView genus) {
        return genus == null || species.belongsToGenus(genus.name());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(species, "species")
                .behavioralCollection(images, "images");
    }
}
