package com.naturalist.garden;

import com.naturalist.ddd.BehavioralCollection;
import com.naturalist.plants.PlantRankName;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Multi-result return type for {@link Planting} queries (ADR-011) — a bed's plantings, or one
 * taxon's plantings across beds and seasons.
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
     * The distinct taxa represented, in encounter order, each at whatever rank its planting
     * recorded — so a bed of unlabelled salvia starts reports a genus here, not nothing. A
     * mixed row reports every one of them; the collection never collapses a bed to a single
     * plant. Plantings identified only by cultivar contribute nothing, their variety being
     * on the planting itself.
     */
    public List<PlantRankName> plants() {
        return stream().map(Planting::plantName).filter(Objects::nonNull).distinct().toList();
    }
}
