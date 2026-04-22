package com.naturalist.sensors;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.measurements.DepthInches;
import com.naturalist.observability.Constraints;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * A catalog entry for a soil monitoring sensor installed at a specific location
 * and depth within a managed spatial unit.
 * <p>
 * {@code Sensor} is a stable named classification that exists independently of any
 * particular reading. It carries the physical installation context needed to interpret
 * readings correctly: which spatial unit it monitors, at what depth, using what
 * hardware model, and whether it has been relocated since installation.
 * <p>
 * <b>Calibration note:</b> The Ecowitt WH51 and WH51L sensors are factory-calibrated
 * for mineral soil. The Oak Vista worm casting / coco coir blend reads 3–5% high
 * relative to true volumetric moisture. The {@code customCalibrationMode} flag
 * indicates whether the sensor has been recalibrated in the Ecowitt app using
 * the zero-moisture and saturation AD reference points. Until custom calibration is
 * applied, {@link SensorReading#adValue()} should be preferred over
 * {@link SensorReading#moisturePercent()} for cross-bed comparisons.
 * <p>
 * <b>Relocation tracking:</b> When a sensor is physically moved — as occurred with
 * the backyard WH51 on approximately April 9, 2026 after air gap formation in
 * rototilled soil caused dropout — the {@code lastRelocatedDate} is recorded.
 * Any time series analysis spanning a relocation date is flagged as potentially
 * non-comparable in the sensor analysis domain.
 * <p>
 * <b>Oak Vista installed sensors (April 2026):</b>
 * <ul>
 *   <li>{@code "box1-shallow"} — WH51, Box 1, ~5 inch, installed April 6, 2026.
 *       Factory calibration. Baseline: 52% (AD 251).</li>
 *   <li>{@code "backyard-shallow"} — WH51, backyard, ~5 inch, installed April 6,
 *       2026. Relocated ~April 9 (air gap post-rototill). New baseline pending
 *       stabilisation. Last reading at relocation: 73% (AD 325).</li>
 *   <li>{@code "backyard-ec"} — WH52, backyard, shallow. Pending arrival ~April 14.
 *       Will provide EC and temperature in addition to moisture.</li>
 *   <li>{@code "box1-deep"} — WH51L, Box 1, 10–12 inch. Pending arrival next week.
 *       Body must be positioned ≥ 20 cm above soil surface for RF transmission.</li>
 *   <li>{@code "backyard-deep"} — WH51L, backyard, 10–12 inch. Pending arrival.</li>
 * </ul>
 */
public record Sensor(
        SensorName name,
        SensorModel model,
        ZoneName zoneName,
        @Nullable SubZoneName subZoneName,
        DepthInches depthInches,
        LocalDate installedDate,
        @Nullable LocalDate lastRelocatedDate,
        boolean customCalibrationMode,
        @Nullable Integer adValueAtZeroMoisture,
        @Nullable Integer adValueAtSaturation,
        @Nullable String notes
) implements NamedEntity<SensorName> {

    // ── Domain queries ─────────────────────────────────────────────────────────

    /**
     * Whether this sensor measures electrical conductivity in addition to moisture.
     * <p>
     * Only the WH52 model provides EC data. The presence of EC capability determines
     * whether {@link SensorReading#ecDsPerMeter()} will be populated for readings
     * from this sensor.
     *
     * @return {@code true} if this sensor's model includes EC measurement
     */
    public boolean measuresEc() {
        return model == SensorModel.WH52;
    }

    /**
     * Whether this sensor is capable of deep-profile installation via a cable probe.
     * <p>
     * The WH51L uses a 1-metre PVC cable to separate the electronics body from the
     * probe, enabling burial at depths up to 80 cm. The WH51 and WH52 are inserted
     * directly and are limited to shallow measurements.
     *
     * @return {@code true} if this sensor uses a cable-separated probe
     */
    public boolean isDeepProbeCapable() {
        return model == SensorModel.WH51L;
    }

    /**
     * Whether this sensor has been relocated since its original installation.
     * <p>
     * A relocation event introduces a discontinuity in the time series — readings
     * before and after the relocation date may not be directly comparable if the
     * new location has different soil characteristics or depth.
     *
     * @return {@code true} if {@code lastRelocatedDate} is set
     */
    public boolean hasBeenRelocated() {
        return lastRelocatedDate != null;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notNull(this, Sensor::model, "model")
                .notNull(this, Sensor::zoneName, "zoneName")
                .namedValue(this, Sensor::depthInches, "depthInches")
                .notNull(this, Sensor::installedDate, "installedDate");
    }

    // ── Hardware model enum ────────────────────────────────────────────────────

    /**
     * The Ecowitt sensor hardware model.
     * <p>
     * Model determines measurement capabilities, probe geometry, IP rating,
     * and installation constraints.
     */
    public enum SensorModel {

        /**
         * Ecowitt WH51 — shallow wireless soil moisture sensor.
         * <p>
         * Capacitance probe inserted directly into soil to 4–6 inch depth.
         * Measures moisture (% and AD) only. IP66 waterproof. Factory calibration
         * for mineral soil reads 3–5% high in worm casting blend.
         * Transmits every 72 seconds. Range up to 100 m open air.
         */
        WH51,

        /**
         * Ecowitt WH51L — deep-probe wireless soil moisture sensor with LCD display.
         * <p>
         * 1-metre PVC cable separates the electronics body from the capacitance probe,
         * enabling measurement at depths up to 80 cm. IP68 probe, IP65 body.
         * Electronics body must be positioned ≥ 20 cm above soil surface to preserve
         * 915 MHz RF transmission. Shares sensor channels with WH51 in the gateway
         * (max 8 combined across WH51 and WH51L). Pending installation at Oak Vista.
         */
        WH51L,

        /**
         * Ecowitt WH52 — 3-in-1 soil moisture, temperature, and EC sensor.
         * <p>
         * Provides electrical conductivity (dS/m) in addition to moisture (% and AD)
         * and soil temperature (°C). EC measurement enables real-time salt
         * accumulation monitoring — critical at Oak Vista following combined
         * potassium sulfate, blood meal, and gypsum applications in April 2026.
         * Pending arrival approximately April 14, 2026. To be installed in the
         * backyard bed where amendment EC impact is highest.
         */
        WH52
    }
}
