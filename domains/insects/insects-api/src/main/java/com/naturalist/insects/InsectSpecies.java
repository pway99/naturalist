package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Species-rank permit of {@link InsectTaxon} — the canonical "we know exactly what species this is" taxon.
 * Composes the species record with the photographic field record. Root identity is
 * the species's {@link InsectSpeciesName}.
 */
public record InsectSpecies(
        InsectSpeciesInfo info,
        ImageCollection images
) implements InsectTaxon {

    public static InsectSpecies of(InsectSpeciesInfo species, ImageCollection images) {
        return new InsectSpecies(species, images);
    }

    public static InsectSpecies of(InsectSpeciesInfo species) {
        return new InsectSpecies(species, ImageCollection.empty());
    }

    @Override
    public InsectSpeciesName name() {
        return info.name();
    }

    /** The genus this species belongs to, exposed as a typed FK delegate. */
    public InsectGenusName genusName() {
        return info.genusName();
    }

    /**
     * True iff this species's genus FK equals the given genus's name, OR the
     * given genus is null. Null tolerance lets a caller compose this check
     * inside a {@code whenNotNull(species, ...)} block without firing a
     * redundant {@code isTrue} violation when the genus is missing.
     */
    public boolean belongsToGenus(@Nullable InsectGenus genus) {
        return genus == null || info.belongsToGenus(genus.name());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(info, "info")
                .behavioralCollection(images, "images");
    }
}
