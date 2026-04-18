package com.naturalist.weather;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.FactName;

import java.util.UUID;

public final class PrecipitationEventName extends FactName {

    private PrecipitationEventName(UUID value) {
        super(value);
    }

    @JsonCreator
    public static PrecipitationEventName of(UUID value) {
        return new PrecipitationEventName(value);
    }
}
