package com.naturalist.garden;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;
import com.naturalist.plants.cultivar.CultivarName;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A crop type with everything garden knows about growing it — the assembled counterpart to
 * {@link CropType}, composed on read by {@code GardenPlanFactory} and never stored.
 * <p>
 * <b>The varieties are derived, not catalogued.</b> {@link #cultivarsPlanted()} reports the
 * cultivars this type's plantings actually name. Garden keeps no list of "varieties available as
 * tomato" — that list belongs to the plants domain, and the interesting question here is what went
 * in the ground, not what could have.
 */
public record GardenPlan(
        CropType cropType,
        PlantingCollection plantings
) implements ReadModel {

    public GardenPlan {
        plantings = plantings == null ? PlantingCollection.empty() : plantings;
    }

    /** The natural key of this plan's crop type. */
    public CropTypeName cropTypeName() {
        return cropType.name();
    }

    /** The varieties actually planted as this type, in encounter order, without duplicates. */
    public List<CultivarName> cultivarsPlanted() {
        return plantings.stream()
                .map(Planting::cultivarName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** The plantings of this type still in the ground on the given date. */
    public PlantingCollection activeOn(LocalDate asOf) {
        return plantings.activeOn(asOf);
    }

    /**
     * Whether this type has ever been planted. False is a real and useful state: lettuce is a crop
     * type Oak Vista soil-tests for before the crop goes in.
     */
    public boolean hasBeenPlanted() {
        return !plantings.isEmpty();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(cropType, "cropType")
                .behavioralCollection(plantings, "plantings");
    }
}
