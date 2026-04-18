package com.naturalist.zone;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Permanent physical infrastructure within a Zone that constrains and enables management decisions.
 * <p>
 * Oak Vista infrastructure inventory (April 2026):
 * <ul>
 *   <li><b>Garden Box 1</b> — WH51 soil moisture sensor, hand-watered, no trellis.</li>
 *   <li><b>Backyard garden</b> — WH51 sensor, hand-watered, no trellis.</li>
 *   <li><b>Passion fruit fence</b> — trellis (fence line), no sensor, moderate traffic.</li>
 *   <li><b>Apiary</b> — no soil sensor, no trellis, low traffic.</li>
 * </ul>
 */
public record Infrastructure(
        boolean hasSoilSensor,
        boolean hasTrellis,
        boolean highTrafficArea,
        boolean hasDripIrrigation,
        boolean hasWindbreak
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> {};
    }
}
