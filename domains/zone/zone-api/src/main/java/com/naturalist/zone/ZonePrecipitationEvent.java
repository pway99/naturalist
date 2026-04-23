package com.naturalist.zone;

import com.naturalist.ddd.Entity;
import com.naturalist.measurements.PrecipitationInches;
import com.naturalist.observability.Constraints;
import com.naturalist.weather.PrecipitationEventId;
import com.naturalist.zone.subzone.SubZoneName;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * The zone domain's local copy of a precipitation event, scoped to a zone or sub-zone.
 * <p>
 * This record represents what the zone domain's weather event handler will persist when
 * a {@code com.naturalist.weather.PrecipitationEvent} is received. The zone domain's
 * interest in precipitation is ecological and microclimate-oriented: precipitation
 * affects thrips and other pest populations, modifies surface habitat conditions,
 * suppresses or spreads disease vectors, and alters the thermal and moisture
 * microclimate that the zone's crops experience.
 * <p>
 * Each domain that subscribes to weather events keeps its own copy because each domain
 * applies domain-specific interpretation. The zone domain's handler has the opportunity
 * to act on the event per-zone — updating pest pressure scores, flagging microclimate
 * changes, or noting irrigation triggering conditions — rather than applying a single
 * property-wide response.
 * <p>
 * <b>Correlation:</b> {@code weatherEventName} references the originating
 * {@code com.naturalist.weather.PrecipitationEvent} by its slug.
 * It is {@code null} for events recorded before the event bus is operational.
 * <p>
 * <b>Zone aggregate wiring:</b> This type exists in anticipation of the event handler
 * infrastructure. Wiring {@code ZonePrecipitationEvent} into the {@link Zone} aggregate
 * (as a {@code List<ZonePrecipitationEvent>}) is deferred until the event bus is
 * operational and the zone handler's full domain logic is defined.
 */
public record ZonePrecipitationEvent(
        ZonePrecipitationEventId name,
        ZoneName zoneName,
        @Nullable SubZoneName subZoneName,
        LocalDate startDate,
        LocalDate endDate,
        PrecipitationInches totalInches,
        Duration totalDuration,
        BigDecimal peakIntensityInchesPerHour,
        @Nullable PrecipitationEventId weatherEventName,
        @Nullable String notes
) implements Entity<ZonePrecipitationEventId> {

    /**
     * Whether this event constitutes a significant surface habitat disruption event
     * for thrips and other surface-dwelling pests.
     * <p>
     * Precipitation above 0.5 inches delivered slowly (≤ 0.3 inches/hour) saturates
     * surface mulch and soil, disrupting the protected inter-layer void spaces that
     * thrips use for overwintering and early-season population establishment.
     * Significant rainfall events are thus natural thrips suppression events and
     * should be factored into pest pressure assessments.
     *
     * @return {@code true} if this event likely caused surface habitat disruption
     */
    public boolean isSignificantHabitatDisruptionEvent() {
        return totalInches.isAtLeast(new BigDecimal("0.5"))
                && peakIntensityInchesPerHour.compareTo(new BigDecimal("0.3")) <= 0;
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
                .entityId(name, "name")
                .notNull(this, ZonePrecipitationEvent::zoneName, "zoneName")
                .notNull(this, ZonePrecipitationEvent::startDate, "startDate")
                .notNull(this, ZonePrecipitationEvent::endDate, "endDate")
                .namedValue(this, ZonePrecipitationEvent::totalInches, "totalInches")
                .notNull(this, ZonePrecipitationEvent::totalDuration, "totalDuration")
                .notNull(this, ZonePrecipitationEvent::peakIntensityInchesPerHour, "peakIntensityInchesPerHour");
    }
}
