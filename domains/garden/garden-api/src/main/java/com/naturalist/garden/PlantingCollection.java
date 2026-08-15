package com.naturalist.garden;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.plants.PlantName;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Multi-result return type for {@link Planting} queries (ADR-011) — a bed's plantings, or one
 * species' plantings across beds and seasons.
 */
public final class PlantingCollection extends BehavioralCollection<Planting> {

    PlantingCollection(Collection<Planting> plantings) {
        super(plantings);
    }

    public static PlantingCollection of(Collection<Planting> plantings) {
        return new PlantingCollection(plantings);
    }

    public static PlantingCollection empty() {
        return new PlantingCollection(List.of());
    }

    /** Only the plantings in the ground on the given date. */
    public PlantingCollection activeOn(LocalDate asOf) {
        return new PlantingCollection(stream().filter(p -> p.isActive(asOf)).toList());
    }

    /**
     * The distinct species represented, in encounter order. A mixed row reports every one of them —
     * the collection never collapses a bed to a single plant.
     */
    public List<PlantName> plants() {
        return stream().map(Planting::plantName).filter(Objects::nonNull).distinct().toList();
    }
}
