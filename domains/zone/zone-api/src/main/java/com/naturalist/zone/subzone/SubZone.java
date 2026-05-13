package com.naturalist.zone.subzone;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.measurements.AreaSquareFeet;
import com.naturalist.observability.Constraints;
import com.naturalist.soil.SoilProfileName;
import com.naturalist.zone.ZoneName;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A named spatial subdivision within a Zone, representing a section of a bed or plot that
 * has distinct management characteristics, pest history, or current occupants.
 * <p>
 * SubZone is an entity within the Zone aggregate — it does not exist independently of
 * its parent Zone. A {@link SubZoneName} is always interpreted relative to the parent
 * {@link com.naturalist.zone.ZoneName}. Zone is the aggregate root.
 * <p>
 * <b>Origin of this concept:</b> SubZone emerged as a first-class domain object from a real
 * management event on April 7, 2026. A TSWV/thrips outbreak in the Oak Vista backyard garden
 * row required treating specific sections differently: infected plants pulled from the north
 * end, Nick's Italian Pear protected in the centre, and Amish Paste starts monitored in the
 * south. The Zone granularity was too coarse to capture this spatial differentiation.
 * This is canonical DDD — ubiquitous language and model evolving together from real domain events.
 * <p>
 * <b>What SubZone enables:</b>
 * <ul>
 *   <li><b>Pest pressure history at sub-zone level</b> — which section of which bed had
 *       TSWV in 2026? This is required for rotation planning in 2027.</li>
 *   <li><b>Surface habitat risk tracking</b> — thrips overwintering risk of the current
 *       surface material, updated by the application layer when the soil domain records
 *       a mulch change.</li>
 *   <li><b>Soil profile association</b> — soft reference to the {@code SoilProfile}
 *       aggregate in the soil domain, enabling the application layer to correlate
 *       sensor readings, lab analyses, and amendment history with a specific sub-zone.</li>
 *   <li><b>Sensor correlation</b> — WH51L deep probes may reveal different moisture dynamics
 *       in different parts of the same bed; SubZones provide the spatial anchoring.</li>
 *   <li><b>Crop rotation planning</b> — the future CropRotation application module reads
 *       SubZone pest history to generate planting recommendations.</li>
 * </ul>
 * <p>
 * <b>Surface material and soil ownership:</b> The identity and physical management of
 * surface materials (mulch type, depth, application history) belongs to the soil domain
 * — {@code MulchType} and {@code MulchLayer} live in soil-api. Zone-api captures only
 * the ecological consequence via {@link ThripsHabitatRisk}, updated by the application
 * layer when the soil domain records a mulch change. This preserves the DAG dependency
 * direction: soil → zone (valid); zone → soil (forbidden cycle).
 * <p>
 * <b>Plant occupants:</b> SubZone will carry soft references to current plant occupants
 * via the Plants module identifier. {@code PlantName} will be added once the Plants
 * module identifier is registered in {@code domains/identifiers/}.
 */
public record SubZone(
        SubZoneName name,
        ZoneName parentZoneName,
        RelativePosition position,
        AreaSquareFeet areaSqft,
        ThripsHabitatRisk surfaceHabitatRisk,
        @Nullable SoilProfileName soilProfileName,
        List<PestPressureRecord> pestPressureHistory
) implements NamedEntity<SubZoneName> {

    public SubZone withSoilProfileName(@Nullable SoilProfileName soilProfileName) {
        return new SubZone(name, parentZoneName, position, areaSqft, surfaceHabitatRisk,
                soilProfileName, pestPressureHistory);
    }

    public SubZone withSurfaceHabitatRisk(ThripsHabitatRisk surfaceHabitatRisk) {
        return new SubZone(name, parentZoneName, position, areaSqft, surfaceHabitatRisk,
                soilProfileName, pestPressureHistory);
    }

    public SubZone withPestPressureHistory(List<PestPressureRecord> pestPressureHistory) {
        return new SubZone(name, parentZoneName, position, areaSqft, surfaceHabitatRisk,
                soilProfileName, pestPressureHistory);
    }

    // ── Domain queries ────────────────────────────────────────────────────────

    /**
     * Whether this SubZone has ever had documented TSWV pressure in any season.
     * <p>
     * Used by the CropRotation application module to flag SubZones where Solanaceae
     * planting should be avoided in the following season.
     *
     * @return {@code true} if any {@link PestPressureRecord.Pathogen#TSWV} record exists
     */
    public boolean hasTswvHistory() {
        return pestPressureHistory.stream()
                .anyMatch(r -> r.pathogen() == PestPressureRecord.Pathogen.TSWV);
    }

    /**
     * Whether this SubZone currently has unresolved pest pressure in the given season.
     *
     * @param season the growing season (calendar year) to query
     * @return {@code true} if any pressure record in the given season is unresolved
     */
    public boolean hasActivePressure(int season) {
        return pestPressureHistory.stream()
                .anyMatch(r -> r.season() == season && !r.resolved());
    }

    /**
     * Returns a new SubZone with the given pest pressure record appended to the history.
     *
     * @param record the new pressure record to append; must not be null
     * @return a new SubZone with the record added at the end of {@code pestPressureHistory}
     */
    public SubZone withAddedPestPressureRecord(PestPressureRecord record) {
        var updated = new ArrayList<>(pestPressureHistory);
        updated.add(record);
        return withPestPressureHistory(List.copyOf(updated));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .entityName(parentZoneName, "parentZoneName")
                .entityNameOrNull(soilProfileName, "soilProfileName")
                .notNull(position, "position")
                .namedValue(areaSqft, "areaSqft")
                .notNull(surfaceHabitatRisk, "surfaceHabitatRisk");
    }
}
