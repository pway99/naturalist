package com.naturalist.zone;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

public record SubstrateCharacteristics(
        SubstrateCategory category,
        boolean hasClaySublayer,
        DrainageCharacteristic drainageCharacteristic,
        BiologicalAmplificationFactor biologicalAmplificationFactor
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(this, SubstrateCharacteristics::category, "category")
                .notNull(this, SubstrateCharacteristics::drainageCharacteristic, "drainageCharacteristic")
                .namedValue(this, SubstrateCharacteristics::biologicalAmplificationFactor, "biologicalAmplificationFactor");
    }
}
