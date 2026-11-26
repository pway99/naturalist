package com.naturalist.plants;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * What {@link PlantQuery#findByName(PlantRankName)} answered: the name asked for, the name
 * actually found, and the composed {@link Plant} for the found name.
 *
 * <p>{@link #exact()} is false when the catalog does not hold the asked name and the query
 * answered the nearest plant it does hold instead.
 */
public record PlantMatch(PlantRankName asked, PlantRankName found, Plant plant) implements ValueObject {

    /** True when the catalog holds the very name asked for. */
    public boolean exact() {
        return asked.equals(found);
    }

    /** True when the plant carries {@code found} as its most specific rung. */
    boolean plantIsFound() {
        return plant != null && found != null && plant.identifiedTo().filter(found::equals).isPresent();
    }

    /** True when {@code found} is {@code asked} itself or a broader rank than it. */
    boolean foundIsAskedOrAbove() {
        return asked != null && found != null
                && (found.equals(asked) || found.rank().compareTo(asked.rank()) < 0);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .identifier(asked, "asked")
                .identifier(found, "found")
                .readModel(plant, "plant")
                .isTrue(plantIsFound(), "plantIsFound")
                .isTrue(foundIsAskedOrAbove(), "foundIsAskedOrAbove");
    }
}
