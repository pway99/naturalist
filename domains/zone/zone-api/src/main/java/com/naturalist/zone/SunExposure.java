package com.naturalist.zone;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

public record SunExposure(
        SunExposureCategory category,
        int estimatedDailyHours,
        boolean hasAfternoonShade
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> {
        };
    }
}
