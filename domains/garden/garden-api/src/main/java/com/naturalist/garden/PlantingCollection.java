package com.naturalist.garden;

import com.naturalist.ddd.BehavioralCollection;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Multi-result return type for {@link Planting} queries (ADR-011) — a crop type's plantings, or a
 * zone's.
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
     * The distinct crop types represented, in encounter order. A mixed row of lettuce and kale
     * reports both — the collection never collapses a bed to a single crop.
     */
    public List<CropTypeName> cropTypes() {
        return stream().map(Planting::cropTypeName).distinct().toList();
    }
}
