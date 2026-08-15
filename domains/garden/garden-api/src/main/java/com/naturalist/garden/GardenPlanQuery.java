package com.naturalist.garden;

import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;

import java.util.Optional;

/**
 * Read port for the assembled {@link GardenPlan}. Standalone rather than an {@code EntityQuery}
 * (mirroring {@code SoilProfileQuery}): the plan has no stored identity of its own, being composed
 * on read from the plantings of a place.
 * <p>
 * Two scopes, because a bed is usefully read at two grains. {@code getByZoneName} takes the whole
 * zone including everything in its sub-zones; {@code getBySubZoneName} takes one subdivision — the
 * useful unit when a zone holds five boxes.
 * <p>
 * Both return empty for a place garden knows nothing about. With no zone catalog of its own, garden
 * cannot tell an unplanted bed from a name that is not a bed at all, and inventing an empty plan
 * for either would assert more than it knows.
 */
public interface GardenPlanQuery {

    Optional<GardenPlan> getByZoneName(ZoneName zoneName);

    Optional<GardenPlan> getBySubZoneName(ZoneName zoneName, SubZoneName subZoneName);
}
