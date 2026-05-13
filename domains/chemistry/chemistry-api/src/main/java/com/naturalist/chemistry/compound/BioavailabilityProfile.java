package com.naturalist.chemistry.compound;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * How a compound is absorbed by biological systems.
 * <p>
 * Owned inline by {@link CompoundInfo} as a value object. Every compound in the
 * catalog has a bioavailability profile — absorption behaviour is an intrinsic
 * property of the compound, not a separately identifiable thing.
 */
public record BioavailabilityProfile(
        AbsorptionPathway primaryPathway,
        BigDecimal relativeAbsorptionRate,  // 0.0-1.0, 1.0 = fastest known
        boolean cuticular,                  // can penetrate waxy cuticle
        boolean stomatal,                   // absorbed through stomata
        boolean isChelateEnhanced,          // chelation improves uptake
        String mechanism                    // plain language description
) implements ValueObject {

    public boolean isFoliarEffective() {
        return cuticular || stomatal;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(primaryPathway, "primaryPathway")
                .notNull(relativeAbsorptionRate, "relativeAbsorptionRate")
                .notNull(mechanism, "mechanism");
    }

    public enum AbsorptionPathway {
        ROOT_MASS_FLOW,      // carried to roots by transpiration stream
        ROOT_DIFFUSION,      // diffuses to roots along concentration gradient
        FOLIAR_STOMATAL,     // enters through stomata
        FOLIAR_CUTICULAR,    // penetrates waxy cuticle
        FOLIAR_BOTH,         // stomatal + cuticular (chelates)
        VOLATILIZATION,      // vapor phase — fumigants
        CONTACT              // kills on contact, no systemic absorption
    }
}
