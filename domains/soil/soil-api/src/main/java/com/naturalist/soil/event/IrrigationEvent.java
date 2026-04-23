package com.naturalist.soil.event;

import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * An immutable record of a deliberate irrigation event applied to a specific spatial target.
 * <p>
 * {@code IrrigationEvent} is a child entity within the {@link com.naturalist.soil.SoilProfile}
 * aggregate. It records manual or intentional irrigation distinct from automated drip
 * irrigation, which is tracked by sensor-derived watering events in the sensor domain.
 * <p>
 * The primary agronomic value of this record is in tracking leaching irrigation events —
 * deliberate deep waterings that move excess soluble salts or nitrogen below the root zone.
 * After the blood meal nitrogen over-application in Box 1 (April 2026), leaching irrigation
 * was used to reduce the elevated nitrate concentration in the root zone.
 * <p>
 * <b>Drainage recovery context:</b> The backyard garden's native clay sublayer restricts
 * downward drainage. A large irrigation event (leaching or otherwise) in this bed has an
 * extended drainage recovery time — the April 2–10, 2026 sensor data showed drainage
 * recovery of >12 hours post-rototill, with an irrigation event during that period
 * extending the saturated state. {@code IrrigationEvent} records provide context for
 * interpreting these drainage recovery trajectories from sensor data.
 */
public record IrrigationEvent(
        IrrigationEventId name,
        ZoneName zoneName,
        @Nullable SubZoneName subZoneName,
        BigDecimal volumeGallons,
        LocalDate appliedDate,
        boolean leachingIrrigation,
        @Nullable String notes
) implements Entity<IrrigationEventId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(name, "name")
                .notNull(this, IrrigationEvent::zoneName, "zoneName")
                .notNull(this, IrrigationEvent::volumeGallons, "volumeGallons")
                .notNull(this, IrrigationEvent::appliedDate, "appliedDate");
    }
}
