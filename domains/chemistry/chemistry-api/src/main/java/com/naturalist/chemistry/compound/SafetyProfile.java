package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.TemperatureFahrenheit;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Safety thresholds and application constraints for a compound.
 * <p>
 * Not all compounds need a {@code SafetyProfile} — only those with meaningful
 * hazard. Gypsum, Epsom salt, insecticidal soap at label rates carry no
 * {@code SafetyProfile}. Formic acid, oxalic acid, and azadirachtin require one.
 * <p>
 * Owned inline by {@link CompoundInfo} as a nullable value object. There is no
 * independent repository — safety characteristics have no meaning outside the
 * compound they describe.
 */
public record SafetyProfile(
        HazardLevel hazardLevel,
        BigDecimal maxSafeConcentrationPpm,
        @Nullable TemperatureFahrenheit minApplicationTempF,
        @Nullable TemperatureFahrenheit maxApplicationTempF,
        boolean requiresProtectiveEquipment,
        boolean hazardousToBeesWhenWet,
        boolean requiresEveningApplication,
        String applicationConstraints
) implements ValueObject {

    public boolean isWithinTemperatureWindow(TemperatureFahrenheit temp) {
        boolean aboveMin = minApplicationTempF == null || !temp.isBelow(minApplicationTempF);
        boolean belowMax = maxApplicationTempF == null || !temp.isAbove(maxApplicationTempF);
        return aboveMin && belowMax;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(hazardLevel, "hazardLevel")
                .notNull(maxSafeConcentrationPpm, "maxSafeConcentrationPpm")
                .notNull(applicationConstraints, "applicationConstraints");
    }

    public enum HazardLevel {
        NONE,      // gypsum, Epsom salt — completely safe
        LOW,       // neem oil, insecticidal soap — mild irritant
        MODERATE,  // oxalic acid — irritant, harmful if ingested
        HIGH,      // formic acid — corrosive, dangerous vapor
        EXTREME    // not currently in Oak Vista catalog
    }
}
