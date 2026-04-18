package com.naturalist.soil.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.FactName;

import java.util.UUID;

public final class SoilPrecipitationEventName extends FactName {

    private SoilPrecipitationEventName(UUID value) {
        super(value);
    }

    @JsonCreator
    public static SoilPrecipitationEventName of(UUID value) {
        return new SoilPrecipitationEventName(value);
    }
}
