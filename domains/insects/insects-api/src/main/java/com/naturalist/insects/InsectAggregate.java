package com.naturalist.insects;

import com.naturalist.ddd.Aggregate;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * The catalog-view aggregate for an insect at Oak Vista — its species record and the
 * photographic field record assembled into a single consistency boundary.
 *
 * <p>{@link InsectSpecies} is the aggregate root; the aggregate's identity is the
 * species's {@link InsectSpeciesName}. Multiple aggregates may share the same root in
 * the future (e.g. a field-notes aggregate over sightings); this one is the canonical
 * catalog view.
 *
 * <p>{@link ImageCollection} is non-null but may be empty — a species can be
 * catalogued without photographs. The aggregate enforces structural invariants on its
 * children (presence and validity); referential integrity between
 * {@link InsectImage#insectSpeciesName()} and {@link InsectSpecies#name()} is the
 * assembly factory's responsibility, since the factory queries images by species name
 * and the match is tautological at construction time.
 */
public record InsectAggregate(
        InsectSpecies species,
        ImageCollection images
) implements Aggregate {

    public static InsectAggregate of(InsectSpecies species, ImageCollection images) {
        return new InsectAggregate(species, images);
    }

    public static InsectAggregate of(InsectSpecies species) {
        return new InsectAggregate(species, ImageCollection.empty());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(species, "species")
                .observable(images, "images");
    }
}
