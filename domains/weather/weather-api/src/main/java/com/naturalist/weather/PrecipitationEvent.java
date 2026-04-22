package com.naturalist.weather;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.measurements.PrecipitationInches;
import com.naturalist.observability.Constraints;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * An immutable record of a natural precipitation event at a specific spatial location.
 * <p>
 * {@code PrecipitationEvent} is a weather domain fact — a discrete atmospheric occurrence
 * distinct from climate (long-term threshold knowledge) and from irrigation (deliberate
 * managed water application). It belongs to the weather domain because precipitation is
 * an uncontrolled atmospheric event, not a soil management action.
 * <p>
 * The spatial target is expressed as a {@link ZoneName} (always present) and an optional
 * {@link SubZoneName} when sub-zone granularity is meaningful. Both types are from the
 * {@code identifiers} module — weather-api carries no compile-time dependency on zone-api.
 * <p>
 * <b>Leaching significance:</b> Precipitation with total accumulation ≥ 0.5 inches
 * delivered at intensity below 0.3 inches/hour constitutes a significant leaching
 * event for soluble soil ions — particularly nitrate (NO₃⁻), which is anionically
 * repelled from soil exchange sites and moves freely with soil water. The April 2026
 * rainfall event at Oak Vista (approximately 1.5 inches over 3 days) provided
 * natural leaching that meaningfully reduced the blood meal nitrogen excess and
 * potassium sulfate ionic load from April 6, 2026 amendment applications.
 * <p>
 * <b>Thiobacillus context:</b> Precipitation that maintains soil moisture above 40%
 * sustains Thiobacillus bacterial activity for sulfur oxidation. The April 2026
 * rainfall kept both beds at or above 60% moisture through the period when soil
 * temperature was above the 77°F activation threshold, confirming conditions were
 * favourable for in-situ gypsum formation from elemental sulfur amendments.
 * <p>
 * <b>Sensor interpretation note:</b> A {@code PrecipitationEvent} record provides
 * essential context for interpreting sensor drainage curves. Without this record,
 * the sensor analysis domain cannot distinguish a rainfall-driven moisture peak from
 * an irrigation event, and drainage rate calculations would be attributed to the
 * wrong cause.
 * <p>
 * <b>Oak Vista precipitation record (April 2026):</b>
 * <ul>
 *   <li>Easter weekend through April 9, 2026 — approximately 1.5 inches total.
 *       Estimated Box 1 delivery: ~45 gallons. Backyard delivery: ~93 gallons.
 *       Both beds returned to resting baseline within 48–72 hours, indicating
 *       improving drainage from the April 2, 2026 rototill event.</li>
 * </ul>
 */
public record PrecipitationEvent(
        PrecipitationEventName name,
        ZoneName zoneName,
        @Nullable SubZoneName subZoneName,
        LocalDate startDate,
        LocalDate endDate,
        PrecipitationInches totalInches,
        Duration totalDuration,
        BigDecimal peakIntensityInchesPerHour,
        @Nullable String notes
) implements NamedEntity<PrecipitationEventName> {

    // ── Domain queries ─────────────────────────────────────────────────────────

    /**
     * Whether this precipitation event constitutes a significant leaching event
     * for soluble soil ions.
     * <p>
     * Significant leaching requires both adequate total volume (≥ 0.5 inches)
     * and low intensity (≤ 0.3 inches/hour peak) so that water infiltrates the
     * full soil profile rather than running off the surface. Slow infiltration
     * carries dissolved ions — particularly mobile nitrate — below the primary
     * root zone.
     *
     * @return {@code true} if this event meets the significant leaching criteria
     */
    public boolean isSignificantLeachingEvent() {
        return totalInches.isAtLeast(new BigDecimal("0.5"))
                && peakIntensityInchesPerHour.compareTo(new BigDecimal("0.3")) <= 0;
    }

    /**
     * Estimated volume delivered to a spatial unit of the given area in gallons.
     * <p>
     * Used by the soil domain's nitrogen status computation to estimate the fraction
     * of root-zone nitrate displaced below the active rooting depth by this event.
     *
     * @param targetAreaSqft the area of the spatial unit in square feet
     * @return approximate gallons delivered
     */
    public BigDecimal estimatedVolumeGallons(BigDecimal targetAreaSqft) {
        // 1 inch of rain over 1 sqft ≈ 0.623 gallons
        return totalInches.value()
                .multiply(targetAreaSqft)
                .multiply(new BigDecimal("0.623"));
    }

    /**
     * Whether this event spans multiple calendar days.
     * <p>
     * Multi-day events deliver moisture more slowly and with better infiltration
     * than single-day events of the same total accumulation, increasing leaching
     * effectiveness per inch delivered.
     *
     * @return {@code true} if the event lasted more than one calendar day
     */
    public boolean isMultiDay() {
        return !startDate.equals(endDate);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notNull(this, PrecipitationEvent::zoneName, "zoneName")
                .notNull(this, PrecipitationEvent::startDate, "startDate")
                .notNull(this, PrecipitationEvent::endDate, "endDate")
                .namedValue(this, PrecipitationEvent::totalInches, "totalInches")
                .notNull(this, PrecipitationEvent::totalDuration, "totalDuration")
                .notNull(this, PrecipitationEvent::peakIntensityInchesPerHour, "peakIntensityInchesPerHour");
    }
}
