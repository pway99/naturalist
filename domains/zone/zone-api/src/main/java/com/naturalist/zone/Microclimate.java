package com.naturalist.zone;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * The local atmospheric character of a Zone as distinct from the ambient Chico regional climate.
 * <p>
 * Oak Vista microclimate profiles (April 2026):
 * <ul>
 *   <li><b>Apiary zone</b> — {@code summerThermalRisk = LOW}, {@code windProtected = true},
 *       {@code hasOakCanopy = true}. Morning sun, afternoon shade, canopy buffering.</li>
 *   <li><b>Backyard garden</b> — {@code summerThermalRisk = HIGH}, {@code windProtected = false},
 *       {@code hasOakCanopy = false}. Exposed south-facing bed; tomato pollen viability at risk
 *       above 95°F during Chico's July–August heat events.</li>
 *   <li><b>Garden Box 1</b> — {@code summerThermalRisk = MODERATE}, partial afternoon shade
 *       from house eave.</li>
 * </ul>
 */
public record Microclimate(
        ThermalRisk summerThermalRisk,
        boolean windProtected,
        boolean frostPocketRisk,
        boolean hasOakCanopy
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.notNull(summerThermalRisk, "summerThermalRisk");
    }
}
