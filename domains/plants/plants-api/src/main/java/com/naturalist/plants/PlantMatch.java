package com.naturalist.plants;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;

import java.util.function.Consumer;

/**
 * What {@link PlantQuery#findByName(PlantRankName)} answered: the name asked for, the name
 * actually found, and the composed {@link Plant} for the found name.
 *
 * <p>{@link #exact()} is false when the catalog does not hold the asked name and the query
 * answered the nearest plant it does hold instead.
 */
public record PlantMatch(PlantRankName asked, PlantRankName found, Plant plant) implements ValueObject {

    private static final Observer observer = Observer.forClass(PlantMatch.class);

    /** True when the catalog holds the very name asked for. */
    public boolean exact() {
        return asked.equals(found);
    }

    /** True when the plant carries {@code found} as its most specific rung. */
    boolean plantIsFound() {
        return plant != null && found != null && plant.identifiedTo().filter(found::equals).isPresent();
    }

    /**
     * The genus a species slug names: the segment before its first '-', or the whole slug when
     * it has none. The one place the binomial rule lives. A null or invalid species is refused; the
     * name returned may still not be valid.
     */
    public static PlantGenusName genusOf(PlantSpeciesName species) {
        observer.arguments("genusOf", i -> i
                .entityName(species, "species"))
                .throwWhenInvalid();
        String slug = species.value();
        int dash = slug.indexOf('-');
        return PlantGenusName.of(dash < 0 ? slug : slug.substring(0, dash));
    }

    /** True when {@code found} is {@code asked} itself, or the genus {@code asked}'s binomial names. */
    boolean foundIsAskedOrItsGenus() {
        if (asked == null || found == null) {
            return false;
        }
        return found.equals(asked)
                || asked instanceof PlantSpeciesName species && species.isValid() && found.equals(genusOf(species));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .identifier(asked, "asked")
                .identifier(found, "found")
                .readModel(plant, "plant")
                .isTrue(plantIsFound(), "plantIsFound")
                .isTrue(foundIsAskedOrItsGenus(), "foundIsAskedOrItsGenus");
    }
}
