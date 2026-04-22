package com.naturalist.zone;

import com.naturalist.ddd.Aggregate;
import com.naturalist.measurements.AreaSquareFeet;
import com.naturalist.observability.Constraints;
import com.naturalist.zone.subzone.PestPressureRecord;
import com.naturalist.zone.subzone.SubZone;
import com.naturalist.zone.subzone.SubZoneName;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Aggregate root of the Zones bounded context.
 * <p>
 * A Zone is a named physical space on the Oak Vista property with permanent geographic,
 * solar, and substrate characteristics. Zones exist independently of their occupants —
 * the plants, soil amendments, and pest treatments that operate within a Zone are managed
 * by other domain modules, which reference zones by {@link ZoneName} from the shared
 * identifiers module.
 * <p>
 * <b>Aggregate boundary:</b> Zone owns its {@link ZoneInfo} root entity, all characteristic
 * value objects ({@link SunExposure}, {@link Aspect}, {@link GeographicBoundary},
 * {@link Microclimate}, {@link SubstrateCharacteristics}, {@link Infrastructure}), and the
 * list of {@link SubZone} child entities. External modules access Zone data only through
 * Zone's curated public methods — the internal value objects are not exposed directly.
 * <p>
 * <b>Immutability and state changes:</b> Zone is immutable. Changes to sub-zone state —
 * recording pest pressure, updating mulch type — are made by producing a new Zone instance
 * via {@link #withUpdatedSubZone(SubZone)}. This keeps the aggregate boundary intact and
 * all mutations explicit.
 * <p>
 * Oak Vista zones (as of April 2026):
 * <ul>
 *   <li><b>Garden Box 1</b> — raised bed, 48 sqft, worm casting blend, WH51 sensor,
 *       south-facing, FULL_SUN. Nitrogen over-application active issue (blood meal, April 2026).</li>
 *   <li><b>Backyard garden</b> — in-ground bed, 100 sqft, native clay amended, WH51 sensor,
 *       south-facing, FULL_SUN. TSWV/thrips outbreak active (April 7, 2026).</li>
 *   <li><b>Apiary</b> — oak hive location, east-facing, partial oak canopy, LOW thermal risk.
 *       Saskatraz colony installed April 1, 2026.</li>
 *   <li><b>Passion fruit fence</b> — linear border, trellis, south-facing.</li>
 * </ul>
 */
public record Zone(
        ZoneInfo zoneInfo,
        SunExposure sunExposure,
        Aspect aspect,
        GeographicBoundary boundary,
        Microclimate microclimate,
        SubstrateCharacteristics substrate,
        Infrastructure infrastructure,
        List<SubZone> subZones
) implements Aggregate {

    // ── With methods ──────────────────────────────────────────────────────────

    public Zone withZoneInfo(ZoneInfo zoneInfo) {
        return new Zone(zoneInfo, sunExposure, aspect, boundary, microclimate,
                substrate, infrastructure, subZones);
    }

    public Zone withSunExposure(SunExposure sunExposure) {
        return new Zone(zoneInfo, sunExposure, aspect, boundary, microclimate,
                substrate, infrastructure, subZones);
    }

    public Zone withAspect(Aspect aspect) {
        return new Zone(zoneInfo, sunExposure, aspect, boundary, microclimate,
                substrate, infrastructure, subZones);
    }

    public Zone withBoundary(GeographicBoundary boundary) {
        return new Zone(zoneInfo, sunExposure, aspect, boundary, microclimate,
                substrate, infrastructure, subZones);
    }

    public Zone withMicroclimate(Microclimate microclimate) {
        return new Zone(zoneInfo, sunExposure, aspect, boundary, microclimate,
                substrate, infrastructure, subZones);
    }

    public Zone withSubstrate(SubstrateCharacteristics substrate) {
        return new Zone(zoneInfo, sunExposure, aspect, boundary, microclimate,
                substrate, infrastructure, subZones);
    }

    public Zone withInfrastructure(Infrastructure infrastructure) {
        return new Zone(zoneInfo, sunExposure, aspect, boundary, microclimate,
                substrate, infrastructure, subZones);
    }

    public Zone withSubZones(List<SubZone> subZones) {
        return new Zone(zoneInfo, sunExposure, aspect, boundary, microclimate,
                substrate, infrastructure, subZones);
    }

    // ── Identity delegates ───────────────────────────────────────────────────

    /**
     * The human-readable natural key (slug) of this Zone.
     *
     * @return the ZoneName of this Zone
     */
    public ZoneName zoneName() {
        return zoneInfo.name();
    }

    /**
     * The classification of this Zone.
     *
     * @return the ZoneType (RAISED_BED, IN_GROUND_BED, APIARY etc.)
     */
    public ZoneType zoneType() {
        return zoneInfo.type();
    }

    // ── Geographic delegates ─────────────────────────────────────────────────

    /**
     * The total area of this Zone in square feet.
     *
     * @return area as recorded in {@link GeographicBoundary}
     */
    public AreaSquareFeet areaSqft() {
        return boundary.areaSqft();
    }

    // ── Solar and thermal delegates ──────────────────────────────────────────

    /**
     * The summer thermal stress classification for this Zone in the Chico climate.
     *
     * @return the thermal risk level of this Zone's microclimate
     */
    public ThermalRisk summerThermalRisk() {
        return microclimate.summerThermalRisk();
    }

    /**
     * Whether this Zone receives afternoon shade.
     *
     * @return {@code true} if afternoon shade is present in {@link SunExposure}
     */
    public boolean hasAfternoonShade() {
        return sunExposure.hasAfternoonShade();
    }

    // ── SubZone queries ───────────────────────────────────────────────────────

    /**
     * Finds a SubZone within this Zone by its natural key.
     *
     * @param subZoneName the name to look up
     * @return the matching SubZone, or empty if no SubZone with that name exists
     */
    public Optional<SubZone> subZone(SubZoneName subZoneName) {
        return subZones.stream()
                .filter(s -> s.name().equals(subZoneName))
                .findFirst();
    }

    /**
     * Whether any SubZone in this Zone has documented TSWV pressure in any season.
     *
     * @return {@code true} if any SubZone has TSWV history
     */
    public boolean hasTswvHistory() {
        return subZones.stream().anyMatch(SubZone::hasTswvHistory);
    }

    /**
     * Whether any SubZone in this Zone currently has unresolved pest pressure in the
     * given season.
     *
     * @param season the growing season (calendar year) to query
     * @return {@code true} if any SubZone has unresolved pressure in the given season
     */
    public boolean hasActivePressure(int season) {
        return subZones.stream().anyMatch(s -> s.hasActivePressure(season));
    }

    // ── Aggregate mutations ───────────────────────────────────────────────────

    /**
     * Returns a new Zone with the given SubZone replacing the existing SubZone of the
     * same identity, or appended if no matching SubZone currently exists.
     *
     * @param updated the SubZone to insert or replace; matched by {@link SubZone#name()}
     * @return a new Zone containing the updated SubZone list
     */
    public Zone withUpdatedSubZone(SubZone updated) {
        boolean replaced = subZones.stream()
                .anyMatch(s -> s.name().equals(updated.name()));

        List<SubZone> newSubZones = replaced
                ? subZones.stream()
                        .map(s -> s.name().equals(updated.name()) ? updated : s)
                        .toList()
                : Stream.concat(subZones.stream(), Stream.of(updated)).toList();

        return withSubZones(newSubZones);
    }

    /**
     * Returns a new Zone with a pest pressure record appended to the specified SubZone.
     *
     * @param subZoneName the natural key of the target SubZone
     * @param record      the pest pressure record to append
     * @return a new Zone with the updated SubZone containing the appended record
     * @throws IllegalArgumentException if no SubZone with the given name exists
     */
    public Zone withPestPressureRecord(SubZoneName subZoneName, PestPressureRecord record) {
        SubZone target = subZone(subZoneName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No SubZone named '" + subZoneName + "' in Zone '" + zoneName() + "'"));
        return withUpdatedSubZone(target.withAddedPestPressureRecord(record));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(this, Zone::zoneInfo, "zoneInfo")
                .notNull(this, Zone::sunExposure, "sunExposure")
                .notNull(this, Zone::aspect, "aspect")
                .notNull(this, Zone::boundary, "boundary")
                .notNull(this, Zone::microclimate, "microclimate")
                .notNull(this, Zone::substrate, "substrate")
                .notNull(this, Zone::infrastructure, "infrastructure");
    }
}
