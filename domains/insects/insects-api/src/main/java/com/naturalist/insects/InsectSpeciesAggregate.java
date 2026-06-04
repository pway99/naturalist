package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

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
    public InsectSpeciesName name() {
        return species.name();
    }

    /** The genus this species belongs to, exposed as a typed FK delegate. */
    public InsectGenusName genusName() {
        return species.genusName();
    }

    /** The family this species belongs to, exposed as a typed FK delegate. */
    public InsectFamilyName familyName() {
        return species.familyName();
    }

    /**
     * True iff this species's genus FK equals the given genus's name, OR the
     * given genus is null. Null tolerance lets a caller compose this check
     * inside a {@code whenNotNull(species, ...)} block without firing a
     * redundant {@code isTrue} violation when the genus is missing.
     */
    public boolean belongsToGenus(@Nullable InsectGenusAggregate genus) {
        return genus == null || species.belongsToGenus(genus.name());
    }

    /**
     * True iff this species's family FK equals the given family's name, OR
     * the given family is null. Same null-tolerance rationale as
     * {@link #belongsToGenus}.
     */
    public boolean belongsToFamily(@Nullable InsectFamilyAggregate family) {
        return family == null || species.belongsToFamily(family.name());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(species, "species")
                .behavioralCollection(images, "images");
    }
}
