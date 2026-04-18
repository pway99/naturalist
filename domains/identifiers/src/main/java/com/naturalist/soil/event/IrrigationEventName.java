package com.naturalist.soil.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.FactName;

import java.util.UUID;

public final class IrrigationEventName extends FactName {

    private IrrigationEventName(UUID value) {
        super(value);
    }

    @JsonCreator
    public static IrrigationEventName of(UUID value) {
        return new IrrigationEventName(value);
    }
}
