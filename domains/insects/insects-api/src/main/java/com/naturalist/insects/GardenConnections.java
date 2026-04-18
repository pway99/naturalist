package com.naturalist.insects;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The ecological relationships connecting an insect species to the broader Oak Vista
 * garden community.
 * <p>
 * {@code supportingPlants} lists the plant species that provide nectar, pollen,
 * nesting substrate, or prey resources that sustain this insect. These are currently
 * modelled as descriptive strings. Once the plants catalog is established they will
 * become typed cross-domain references — the string values here should be treated as
 * future {@code PlantName} slugs pending that alignment.
 * <p>
 * {@code relationshipToOtherBeneficials} describes how this species interacts with
 * other beneficial insects at Oak Vista — competitive overlap, complementary
 * microhabitat partitioning, or prey sharing. Predicts management tensions or
 * synergies (e.g. lacewing larvae and ladybug larvae share aphid colonies but occupy
 * different microhabitats).
 * <p>
 * {@code naturalEnemies} documents what preys on this species at Oak Vista —
 * relevant to understanding why beneficial populations fluctuate and what habitat
 * structures support population persistence despite predation pressure.
 */
public record GardenConnections(
        List<String> supportingPlants,
        @Nullable String relationshipToOtherBeneficials,
        @Nullable String naturalEnemies
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.notNull(this, GardenConnections::supportingPlants, "supportingPlants");
    }
}
