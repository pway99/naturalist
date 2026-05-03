package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.TemperatureFahrenheit;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Temperature-dependent volatilization behaviour of a compound.
 * <p>
 * Owned inline by {@link CompoundInfo} as a nullable value object — only volatile
 * compounds (formic acid, thymol, oxalic acid) carry this profile. Non-volatile
 * compounds carry {@code null}.
 */
public record VolatilizationProfile(
        TemperatureFahrenheit minEffectiveTempF,
        TemperatureFahrenheit maxSafeTempF,
        TemperatureFahrenheit optimalTempF,
        BigDecimal vaporPressureAt20C,
        String efficacyNotes,
        String safetyNotes
) implements ValueObject {

    private static final BigDecimal OPTIMAL_WINDOW_DEGREES = BigDecimal.TEN;

    public boolean isEffectiveAt(TemperatureFahrenheit temp) {
        return !temp.isBelow(minEffectiveTempF) && !temp.isAbove(maxSafeTempF);
    }

    public boolean isOptimalAt(TemperatureFahrenheit temp) {
        return temp.isWithin(OPTIMAL_WINDOW_DEGREES, optimalTempF);
    }

    public boolean isSafeAt(TemperatureFahrenheit temp) {
        return !temp.isAbove(maxSafeTempF);
    }

    public TemperatureAssessment assess(TemperatureFahrenheit temp) {
        if (temp.isBelow(minEffectiveTempF)) return TemperatureAssessment.TOO_COLD_INEFFECTIVE;
        if (temp.isAbove(maxSafeTempF)) return TemperatureAssessment.TOO_HOT_DANGEROUS;
        if (isOptimalAt(temp)) return TemperatureAssessment.OPTIMAL;
        return TemperatureAssessment.ACCEPTABLE;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedValue(this, VolatilizationProfile::minEffectiveTempF, "minEffectiveTempF")
                .namedValue(this, VolatilizationProfile::maxSafeTempF, "maxSafeTempF")
                .namedValue(this, VolatilizationProfile::optimalTempF, "optimalTempF")
                .notNull(this, VolatilizationProfile::vaporPressureAt20C, "vaporPressureAt20C")
                .notNull(this, VolatilizationProfile::efficacyNotes, "efficacyNotes")
                .notNull(this, VolatilizationProfile::safetyNotes, "safetyNotes");
    }

    public enum TemperatureAssessment {
        TOO_COLD_INEFFECTIVE,
        ACCEPTABLE,
        OPTIMAL,
        TOO_HOT_DANGEROUS
    }
}
