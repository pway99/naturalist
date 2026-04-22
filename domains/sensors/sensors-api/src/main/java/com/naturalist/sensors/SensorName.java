package com.naturalist.sensors;

import com.naturalist.ddd.EntityName;

/**
 * The natural key (slug) of a {@link Sensor} catalog entity.
 * <p>
 * Stable identifier used for cross-domain references to sensor catalog entries —
 * for example, from the sensor analysis domain mapping XLSX channel names to
 * soil domain sensors. Per ADR-021, cross-entity references use {@code SensorName};
 * the numeric persistence id is adapter-internal.
 * <p>
 * <b>Oak Vista sensor slugs (April 2026):</b>
 * <ul>
 *   <li>{@code "box1-shallow"} — WH51 probe, Box 1, ~5 inch depth</li>
 *   <li>{@code "backyard-shallow"} — WH51 probe, backyard garden, ~5 inch depth</li>
 *   <li>{@code "backyard-ec"} — WH52 (pending ~April 14), backyard, shallow depth</li>
 *   <li>{@code "box1-deep"} — WH51L (pending), Box 1, 10–12 inch depth</li>
 *   <li>{@code "backyard-deep"} — WH51L (pending), backyard, 10–12 inch depth</li>
 * </ul>
 */
public final class SensorName extends EntityName {

    private SensorName(String value) {
        super(value);
    }

    public static SensorName of(String value) {
        return new SensorName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
