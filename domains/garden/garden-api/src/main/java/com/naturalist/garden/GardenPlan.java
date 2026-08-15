package com.naturalist.garden;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A bed and what is growing in it — the plants of one place, assembled on read by
 * {@code GardenPlanFactory} and never stored.
 * <p>
 * <b>Keyed by place, not by crop.</b> A garden plan answers "what is in this bed", which is how a
 * gardener holds it: the back yard is not a tomato plan and an eggplant plan, it is one bed with
 * both in it. Grouping by crop would fragment a bed that is physically one thing, and it is beds
 * that get amended, irrigated and sampled.
 * <p>
 * <b>The place may be a zone or one subdivision of it.</b> {@code subZoneName} null means the plan
 * covers the whole zone, including everything planted in its sub-zones; set, it covers that
 * subdivision alone. The front garden is five boxes in a single zone, and a plan for the zone
 * would lump all five — so the box is the useful unit there, while the back yard is usefully read
 * either whole or row by row. The nullability mirrors {@code SoilProfileInfo} exactly, which is
 * the same distinction applied to sampling rather than planting.
 * <p>
 * The place is a soft {@link ZoneName} / {@link SubZoneName} into the zone domain, and garden holds
 * no root entity of its own for it — a read model needs no identity. Garden therefore cannot
 * distinguish "a bed with nothing planted" from "not a bed at all", which is why the query returns
 * empty rather than an empty plan.
 */
public record GardenPlan(
        ZoneName zoneName,
        @Nullable SubZoneName subZoneName,
        PlantingCollection plantings
) implements ReadModel {

    public GardenPlan {
        plantings = plantings == null ? PlantingCollection.empty() : plantings;
    }

    /** Whether this plan covers a whole zone rather than one subdivision of it. */
    public boolean coversWholeZone() {
        return subZoneName == null;
    }

    /** The distinct species growing here, in encounter order. */
    public List<PlantName> plants() {
        return plantings.plants();
    }

    /** The distinct varieties recorded here, in encounter order. */
    public List<CultivarName> cultivars() {
        return plantings.stream()
                .map(Planting::cultivarName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** What is in the ground on the given date, as against everything ever planted here. */
    public PlantingCollection activeOn(LocalDate asOf) {
        return plantings.activeOn(asOf);
    }

    /** Total plants recorded here; uncounted plantings contribute nothing rather than guessing. */
    public int plantCount() {
        return plantings.stream()
                .map(Planting::plantCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(zoneName, "zoneName")
                .entityNameOrNull(subZoneName, "subZoneName")
                .behavioralCollection(plantings, "plantings");
    }
}
