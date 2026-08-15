package com.naturalist.garden;

import java.util.Optional;

/**
 * Read port for the assembled {@link GardenPlan}. Standalone rather than an {@code EntityQuery}
 * (mirroring {@code SoilProfileQuery}): the plan has no stored identity of its own, being composed
 * on read from a crop type and its plantings.
 */
public interface GardenPlanQuery {

    Optional<GardenPlan> getByCropTypeName(CropTypeName cropTypeName);
}
