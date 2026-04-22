package com.naturalist.sensors;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.measurements.ElectricalConductivity;
import com.naturalist.measurements.MoisturePercent;
import com.naturalist.observability.Constraints;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * An immutable timestamped soil moisture measurement from an Ecowitt capacitance sensor.
 * <p>
 * {@code SensorReading} is a child entity within the {@link com.naturalist.soil.SoilProfile}
 * aggregate. It records both the raw hardware measurement and the firmware-derived
 * volumetric percentage. Because the Oak Vista worm casting / coco coir blend has
 * different dielectric properties than the mineral soil used for factory calibration,
 * the raw {@code adValue} is the preferred basis for custom calibration and
 * cross-bed trend analysis.
 * <p>
 * <b>Sensor hardware at Oak Vista (April 2026):</b>
 * <ul>
 *   <li><b>WH51</b> — shallow probe, 4–6 inch depth, IP66. Factory calibration reads
 *       3–5% high in worm casting blend. Resting baselines: Box 1 ≈ 52% (AD ≈ 251),
 *       Backyard ≈ 59% (AD ≈ 277). Watering triggers: Box 1 = 35%, Backyard = 40%.</li>
 *   <li><b>WH51L</b> — long-cable probe, up to 80 cm depth, IP68. Electronics body
 *       must remain ≥ 20 cm above soil surface to preserve RF transmission. Pending
 *       installation in both beds at 10–12 inch depth.</li>
 *   <li><b>WH52</b> — 3-in-1 sensor (moisture + temperature + EC). Pending arrival
 *       ~April 14. To be installed in backyard bed for EC trend monitoring post
 *       amendment application.</li>
 * </ul>
 * <p>
 * <b>AD value interpretation:</b> The AD (analog-to-digital) value is the raw
 * capacitance count before firmware conversion. It is unaffected by the factory
 * calibration algorithm and is stable across firmware versions. For the Oak Vista
 * worm casting blend, a custom calibration curve correlating AD values against
 * gravimetric moisture samples will produce more accurate volumetric readings than
 * the factory percentage.
 * <p>
 * <b>Drainage recovery classification (from sensor analysis domain):</b>
 * <pre>
 *   &lt; 6 hours to baseline  → HEALTHY    (intact macropore architecture)
 *   6–12 hours             → RECOVERING (partial biological reconstruction)
 *   12–24 hours            → DISRUPTED  (significant macropore disruption)
 *   &gt; 24 hours             → IMPAIRED   (severe disruption or compaction)
 * </pre>
 * Post-rototill (April 2, 2026): backyard measured DISRUPTED at 20–24 hours.
 * By April 9 trend showed improvement toward RECOVERING — biological structure
 * re-establishment in progress.
 */
public record SensorReading(
        SensorReadingName name,
        SensorName sensorName,
        ZoneName zoneName,
        @Nullable SubZoneName subZoneName,
        Instant recordedAt,
        int adValue,
        MoisturePercent moisturePercent,
        @Nullable ElectricalConductivity ecDsPerMeter,
        @Nullable BigDecimal temperatureCelsius,
        @Nullable String notes
) implements NamedEntity<SensorReadingName> {

    // ── Domain queries ─────────────────────────────────────────────────────────

    /**
     * Whether this reading was recorded at or above the Thiobacillus activation
     * temperature threshold of 77°F (25°C).
     * <p>
     * Thiobacillus thiooxidans sulfur oxidation rate increases significantly above
     * 25°C (Q10 ≈ 2). Readings above this threshold contribute to the Thiobacillus
     * Activity Score — the count of above-threshold intervals in a period. At Oak Vista
     * the backyard bed recorded 7 consecutive above-threshold readings April 4–9, 2026
     * (peak 84.8°F April 6 at 13:00), confirming sulfur oxidation was active during
     * the critical post-amendment period.
     *
     * @return {@code true} if temperature is recorded and is ≥ 25.0°C
     */
    public boolean isAboveThiobacillusActivationThreshold() {
        return temperatureCelsius != null && temperatureCelsius.compareTo(new BigDecimal("25.0")) >= 0;
    }

    /**
     * Whether this reading indicates soil moisture is at or below the watering trigger
     * threshold for the given bed type.
     * <p>
     * Trigger thresholds encode the domain knowledge that worm casting blend (Box 1)
     * holds plant-available water at lower volumetric percentages than the clay-amended
     * backyard bed. These values are not physical constants — they are management
     * thresholds calibrated to observed plant stress onset in each substrate.
     *
     * @param triggerThresholdPercent the watering trigger for this sensor's bed
     * @return {@code true} if moisture is at or below the trigger threshold
     */
    public boolean isBelowWateringTrigger(BigDecimal triggerThresholdPercent) {
        return moisturePercent.value().compareTo(triggerThresholdPercent) <= 0;
    }

    /**
     * Whether this reading is approaching the watering trigger threshold —
     * within 5 percentage points above it.
     * <p>
     * Used by the sensor analysis domain to generate TRIGGER_APPROACHING findings
     * before the threshold is actually crossed, enabling proactive irrigation
     * scheduling rather than reactive response to drought stress.
     *
     * @param triggerThresholdPercent the watering trigger for this sensor's bed
     * @return {@code true} if moisture is within 5% above the trigger threshold
     */
    public boolean isApproachingWateringTrigger(BigDecimal triggerThresholdPercent) {
        return moisturePercent.value().compareTo(triggerThresholdPercent) > 0
                && moisturePercent.value().compareTo(triggerThresholdPercent.add(new BigDecimal("5.0"))) <= 0;
    }

    /**
     * Whether this reading has EC data available.
     * <p>
     * EC data is only present for WH52 sensor readings. WH51 and WH51L sensors
     * measure moisture only. Callers must check this before accessing
     * {@link #ecDsPerMeter()}.
     *
     * @return {@code true} if EC data is present
     */
    public boolean hasEcReading() {
        return ecDsPerMeter != null;
    }

    /**
     * Whether this reading indicates a potentially problematic EC level.
     * <p>
     * EC above 2.0 dS/m indicates salt accumulation sufficient to cause osmotic
     * stress in most vegetable crops. At Oak Vista, combined amendment applications
     * (potassium sulfate, blood meal, gypsum) in April 2026 were projected to elevate
     * EC above the pre-amendment FGL baseline of 0.50–0.66 dS/m. The WH52 arrival
     * (~April 14) enables real-time monitoring of EC recovery toward baseline as
     * leaching irrigation and plant uptake reduce the ion load.
     *
     * @return {@code true} if EC is present and exceeds 2.0 dS/m
     */
    public boolean isEcElevated() {
        return ecDsPerMeter != null && ecDsPerMeter.value().compareTo(new BigDecimal("2.0")) > 0;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .entityName(sensorName, "sensorName")
                .notNull(this, SensorReading::zoneName, "zoneName")
                .notNull(this, SensorReading::recordedAt, "recordedAt")
                .namedValue(this, SensorReading::moisturePercent, "moisturePercent");
    }
}
