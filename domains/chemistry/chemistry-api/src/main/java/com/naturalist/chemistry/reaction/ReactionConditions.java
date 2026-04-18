package com.naturalist.chemistry.reaction;

import com.naturalist.chemistry.TemperatureFahrenheit;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The conditions under which a reaction occurs.
 * Temperature range, pH range, moisture requirement, biological catalyst.
 * <p>
 * Agnostic to specific reaction — values from reactions.json.
 * <p>
 * Examples from Oak Vista context:
 * - Sulfur oxidation: requires Thiobacillus bacteria (biological catalyst),
 * accelerates above 77°F, requires moist aerobic soil
 * - Gypsum dissolution: moisture required, temperature-moderate effect,
 * no biological catalyst needed
 * - Formic acid fumigation: temperature-driven vapor pressure,
 * dangerous above 85°F
 */
public record ReactionConditions(
        @Nullable TemperatureFahrenheit minTempF,
        @Nullable TemperatureFahrenheit maxTempF,
        @Nullable TemperatureFahrenheit optimalTempF,
        boolean requiresMoisture,
        boolean requiresAerobicConditions,
        boolean requiresBiologicalCatalyst,
        @Nullable String biologicalCatalyst,
        @Nullable BigDecimal minPh,
        @Nullable BigDecimal maxPh
) implements ValueObject {

    private static final MathContext RATE_PRECISION = new MathContext(4, RoundingMode.HALF_UP);

    /**
     * The biological catalyst as an Optional — empty if no biological catalyst is required.
     */
    public Optional<String> biologicalCatalystOptional() {
        return Optional.ofNullable(biologicalCatalyst);
    }

    public boolean isTemperatureDependent() {
        return minTempF != null || maxTempF != null || optimalTempF != null;
    }

    /**
     * Is the reaction likely occurring at the given temperature?
     * Returns false if temperature is outside the defined range.
     */
    public boolean isActiveAt(TemperatureFahrenheit temp) {
        if (minTempF != null && temp.isBelow(minTempF)) return false;
        if (maxTempF != null && temp.isAbove(maxTempF)) return false;
        return true;
    }

    /**
     * Relative reaction rate at the given temperature.
     * {@code BigDecimal.ZERO} = not occurring. {@code BigDecimal.ONE} = optimal rate.
     * Linear approximation between min, optimal, and max.
     */
    public BigDecimal relativeRateAt(TemperatureFahrenheit temp) {
        if (!isActiveAt(temp)) return BigDecimal.ZERO;
        if (optimalTempF == null) return BigDecimal.ONE;
        BigDecimal t = temp.value();
        BigDecimal opt = optimalTempF.value();
        int cmp = t.compareTo(opt);
        if (cmp == 0) return BigDecimal.ONE;
        if (cmp < 0 && minTempF != null) {
            BigDecimal numerator = t.subtract(minTempF.value());
            BigDecimal denominator = opt.subtract(minTempF.value());
            return numerator.divide(denominator, RATE_PRECISION);
        }
        if (cmp > 0 && maxTempF != null) {
            BigDecimal numerator = maxTempF.value().subtract(t);
            BigDecimal denominator = maxTempF.value().subtract(opt);
            return numerator.divide(denominator, RATE_PRECISION);
        }
        return new BigDecimal("0.5");
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> {
        };
    }
}
