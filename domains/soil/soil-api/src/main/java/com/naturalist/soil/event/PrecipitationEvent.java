package com.naturalist.soil.event;

import com.naturalist.ddd.Entity;
import com.naturalist.measurements.PrecipitationInches;
import com.naturalist.observability.Constraints;
import com.naturalist.weather.PrecipitationEventId;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * The soil domain's local copy of a precipitation event, scoped to a managed soil unit.
 * <p>
 * This record represents what the soil domain's weather event handler will persist when
 * a {@code com.naturalist.weather.PrecipitationEvent} is received from the weather domain.
 * Each domain module that cares about precipitation keeps its own copy, because each has
 * domain-specific context to add — the soil domain relates precipitation to leaching,
 * nutrient displacement, and Thiobacillus activity; the zone domain relates it to pest
 * pressure and microclimate conditions.
 * <p>
 * <b>Correlation:</b> {@code weatherEventName} is the cross-domain correlation key — the
 * {@link PrecipitationEventId} slug of the originating weather-domain event. It is
 * {@code null} for events recorded before the weather event bus is operational, and
 * non-null once the handler infrastructure is in place.
 * <p>
 * <b>Leaching significance:</b> Precipitation with total accumulation ≥ 0.5 inches
 * delivered at intensity below 0.3 inches/hour constitutes a significant leaching
 * event for soluble soil ions — particularly nitrate (NO₃⁻), which moves freely with
 * soil water. The April 2026 rainfall event at Oak Vista (1.5 inches over 3 days)
 * provided natural leaching that meaningfully reduced the blood meal nitrogen excess
 * from the April 6, 2026 amendment applications.
 * <p>
 * <b>Thiobacillus context:</b> Precipitation maintaining soil moisture above 40%
 * sustains Thiobacillus bacterial activity for sulfur oxidation. The April 2026
 * rainfall kept both beds at or above 60% through the period when soil temperature
 * was above the 77°F activation threshold — confirming conditions were favourable
 * for in-situ gypsum formation.
 * <p>
 * <b>Sensor interpretation:</b> A precipitation record provides essential context for
 * drainage curve interpretation. Without it, a rainfall-driven moisture peak cannot
 * be distinguished from an irrigation event.
 */
public record PrecipitationEvent(
        SoilPrecipitationEventId name,
        ZoneName zoneName,
        @Nullable SubZoneName subZoneName,
        LocalDate startDate,
        LocalDate endDate,
        PrecipitationInches totalInches,
        Duration totalDuration,
        BigDecimal peakIntensityInchesPerHour,
        @Nullable PrecipitationEventId weatherEventName,
        @Nullable String notes
) implements Entity<SoilPrecipitationEventId> {

    /** 1 inch of rain over 1 square foot ≈ 0.623 US gallons. */
    private static final BigDecimal GALLONS_PER_INCH_PER_SQFT = new BigDecimal("0.623");
    private static final BigDecimal LEACHING_THRESHOLD_INCHES = new BigDecimal("0.5");
    private static final BigDecimal LEACHING_MAX_INTENSITY = new BigDecimal("0.3");

    // ── Domain queries ─────────────────────────────────────────────────────────

    /**
     * Whether this event constitutes a significant leaching event for soluble soil ions.
     * <p>
     * Requires both adequate total volume (≥ 0.5 inches) and low intensity
     * (≤ 0.3 inches/hour peak) so that water infiltrates the full profile rather
     * than running off. The April 2026 Oak Vista event (1.5 inches over 3 days)
     * meets both criteria — estimated 30–50% root-zone nitrate reduction.
     *
     * @return {@code true} if this event meets the significant leaching criteria
     */
    public boolean isSignificantLeachingEvent() {
        return totalInches.isAtLeast(LEACHING_THRESHOLD_INCHES)
                && peakIntensityInchesPerHour.compareTo(LEACHING_MAX_INTENSITY) <= 0;
    }

    /**
     * Estimated volume delivered to a spatial unit of the given area in gallons.
     * <p>
     * Used by the nitrogen status computation to estimate the fraction of root-zone
     * nitrate displaced below the active rooting depth by this event.
     *
     * @param targetAreaSqft the area of the spatial unit in square feet
     * @return approximate gallons delivered
     */
    public BigDecimal estimatedVolumeGallons(BigDecimal targetAreaSqft) {
        return totalInches.value()
                .multiply(targetAreaSqft)
                .multiply(GALLONS_PER_INCH_PER_SQFT)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Whether this event spans multiple calendar days.
     * <p>
     * Multi-day events deliver moisture with better infiltration than single-day
     * events of the same accumulation, increasing leaching effectiveness per inch.
     *
     * @return {@code true} if the event lasted more than one calendar day
     */
    public boolean isMultiDay() {
        return !startDate.equals(endDate);
    }

    /**
     * Whether this record is correlated to a canonical weather domain event.
     *
     * @return {@code true} if {@code weatherEventName} is set
     */
    public boolean isCorrelated() {
        return weatherEventName != null;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .factName(name, "name")
                .notNull(this, PrecipitationEvent::zoneName, "zoneName")
                .notNull(this, PrecipitationEvent::startDate, "startDate")
                .notNull(this, PrecipitationEvent::endDate, "endDate")
                .namedValue(this, PrecipitationEvent::totalInches, "totalInches")
                .notNull(this, PrecipitationEvent::totalDuration, "totalDuration")
                .notNull(this, PrecipitationEvent::peakIntensityInchesPerHour, "peakIntensityInchesPerHour");
    }
}
